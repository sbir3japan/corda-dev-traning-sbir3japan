package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.training.contracts.IOUContract;
import net.corda.training.states.IOUState;

import java.util.*;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.workflows.GetBalances.getCashBalance;

public class IOUSettleFlow {

    /**
     * This is the flow which handles the settlement (partial or complete) of existing IOUs on the ledger.
     * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
     * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
     * The flow returns the [SignedTransaction] that was committed to the ledger.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier stateLinearId;
        private final String currency;
        private final long amount;

        public InitiatorFlow(UniqueIdentifier stateLinearId, String currency, long amount) {
            this.stateLinearId = stateLinearId;
            this.currency = currency;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Set up a payment account
            Amount<Currency> settleAmount = new Amount<>(amount, Currency.getInstance(currency));

            // 2. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            List<UUID> listOfLinearIds = Arrays.asList(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);

            // 3. Get a reference to the inputState data that we are going to settle.
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToSettle = (IOUState) ((StateAndRef) results.getStates().get(0)).getState().getData();

            // 4. Check the party running this flow is the borrower.
            if (!inputStateToSettle.borrower.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new IllegalArgumentException("The borrower must issue the flow");
            }

            // 5. We should now get some of the components required for to execute the transaction
            // Here we get a reference to the default notary and instantiate a transaction builder.
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder tb = new TransactionBuilder(notary);

            // 6. Check we have enough cash to settle the requested amount
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) settleAmount.getToken());

            if (cashBalance.getQuantity() < settleAmount.getQuantity()) {
                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
            } else if (settleAmount.getQuantity() > (inputStateToSettle.amount.getQuantity() - inputStateToSettle.paid.getQuantity())) {
                throw new IllegalArgumentException("Borrow tried to settle with more than was required for the obligation.");
            }

            // 7. Get some cash from the vault and add a spend to our transaction builder.
            CashUtils.generateSpend(getServiceHub(), tb, settleAmount, getOurIdentityAndCert(), inputStateToSettle.lender, ImmutableSet.of()).getSecond();

            // 8. Create a command. you will need to provide the Command constructor with a reference to the Settle Command as well as a list of required signers.
            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    inputStateToSettle.getParticipants()
                            .stream().map(AbstractParty::getOwningKey)
                            .collect(Collectors.toList())
            );

            // 9. Add the command and the input state to the transaction using the TransactionBuilder.
            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // 10. Add an IOU output state if the IOU in question that has not been fully settled.
            if (settleAmount.getQuantity() < inputStateToSettle.amount.getQuantity()) {
                tb.addOutputState(inputStateToSettle.pay(settleAmount), IOUContract.IOU_CONTRACT_ID);
            }

            // 11. Verify and sign the transaction
            tb.verify(getServiceHub());
            SignedTransaction stx = getServiceHub().signInitialTransaction(tb, getOurIdentity().getOwningKey());

            // 12. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            List<FlowSession> sessions = new ArrayList<>();

            for (AbstractParty participant: inputStateToSettle.getParticipants()) {
                Party partyToInitiateFlow = (Party) participant;
                if (!partyToInitiateFlow.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                    sessions.add(initiateFlow(partyToInitiateFlow));
                }
            }
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(stx, sessions));

            /* 13. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, sessions));

        }

    }

    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;
        private SecureHash txWeJustSignedId;

        public Responder(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().outputsOfType(IOUState.class).get(0);
                        require.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSignedId = stx.getId();
                }
            }

            // Create a sign transaction flow
            SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flow to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSignedId));

        }
    }

    /**
     * Self issues the calling node an amount of cash in the desired currency.
     * Only used for demo/sample/training purposes!
     */

    @InitiatingFlow
    @StartableByRPC
    public static class SelfIssueCashFlow extends FlowLogic<Cash.State> {

        private final String currency;
        private final long amount;

        public SelfIssueCashFlow(String currency, long amount) {
            this.currency = currency;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public Cash.State call() throws FlowException {

            // 1. settle amount
            Amount<Currency> settleAmount = new Amount<>(amount, Currency.getInstance(currency));
            
            // Create the cash issue command.
            OpaqueBytes issueRef = OpaqueBytes.of(new byte[0]);
            // Note: ongoing work to support multiple notary identities is still in progress. */
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            // Create the cash issuance transaction.
            AbstractCashFlow.Result cashIssueTransaction = subFlow(new CashIssueFlow(settleAmount, issueRef, notary));
            return (Cash.State) cashIssueTransaction.getStx().getTx().getOutput(0);
        }

    }

}