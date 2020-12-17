package net.corda.training.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;

import java.util.Currency;

@InitiatingFlow
@StartableByRPC
public class SelfIssueCashFlow extends FlowLogic<Cash.State> {

//    private Amount<Currency> amount;
    private String currency;
    private long amount;

//    public SelfIssueCashFlow(Amount<Currency> amount) {
//        this.amount = amount;
//    }
    public SelfIssueCashFlow(String currency, long amount) {
        this.currency = currency;
        this.amount = amount;
    }

    @Suspendable
    @Override
    public Cash.State call() throws FlowException {

        Amount<Currency> issueAmount = new Amount<>(amount, Currency.getInstance(currency));
        /** Create the cash issue command. */
        OpaqueBytes issueRef = OpaqueBytes.of("1".getBytes());
        /** Note: ongoing work to support multiple notary identities is still in progress. */
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        /** Create the cash issuance transaction. */
//        SignedTransaction cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary)).getStx();
        SignedTransaction cashIssueTransaction = subFlow(new CashIssueFlow(issueAmount, issueRef, notary)).getStx();
        /** Return the cash output. */
        return (Cash.State) cashIssueTransaction.getTx().getOutputs().get(0).getData();
    }

}