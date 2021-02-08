//package net.corda.training.flow;
//
//import co.paralleluniverse.fibers.Suspendable;
//import net.corda.core.contracts.Command;
//import net.corda.core.contracts.ContractState;
//import net.corda.core.crypto.SecureHash;
//import net.corda.core.flows.*;
//import net.corda.core.identity.AbstractParty;
//import net.corda.core.identity.Party;
//import net.corda.core.transactions.SignedTransaction;
//import net.corda.core.transactions.TransactionBuilder;
//import net.corda.core.utilities.ProgressTracker;
//import net.corda.training.contracts.IOUContract;
//import net.corda.training.flow.utilities.InstanceGenerateFlow;
//import net.corda.training.states.IOUState;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static net.corda.core.contracts.ContractsDSL.requireThat;
//import static net.corda.training.contracts.IOUContract.Commands.*;
//
///**
// * This is the flow which handles issuance of new IOUs on the ledger.
// * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
// * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
// * The flow returns the [SignedTransaction] that was committed to the ledger.
// */
//public class IOUIssueFlow {
//
//	@InitiatingFlow(version = 2)
//	@StartableByRPC
//	public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
//
//		private final String currency;
//		private final long amount;
//		private final Party lender;
//		private final Party borrower;
//
//		public InitiatorFlow(String currency, long amount, Party lender, Party borrower) {
//			this.currency = currency;
//			this.amount = amount;
//			this.lender = lender;
//			this.borrower = borrower;
//
//		}
//
//		@Suspendable
//		@Override
//		public SignedTransaction call() throws FlowException {
//
//			// Step 1. create IOUState
//			// Note .Make sure that the Party of the lender and the executing node are equal.
//			if ( !borrower.equals(getOurIdentity())){
//				throw new FlowException("The Party of the borrower and the executing node are different..");
//			}
//			final IOUState state = subFlow(new InstanceGenerateFlow(currency, amount, lender, borrower));
//
//			// Step 2. Get a reference to the notary service on our network and our key pair.
//			// Note: ongoing work to support multiple notary identities is still in progress.
//			final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
//
//			// Step 3. Create a new issue command.
//			// Remember that a command is a CommandData object and a list of CompositeKeys
//			final Command<Issue> issueCommand = new Command<>(
//					new Issue(), state.getParticipants()
//					.stream().map(AbstractParty::getOwningKey)
//					.collect(Collectors.toList()));
//
//			// Step 4. Create a new TransactionBuilder object.
//			final TransactionBuilder builder = new TransactionBuilder(notary);
//
//			// Step 5. Add the iou as an output state, as well as a command to the transaction builder.
//			builder.addOutputState(state, IOUContract.IOU_CONTRACT_ID);
//			builder.addCommand(issueCommand);
//
//
//			// Step 6. Verify and sign it with our KeyPair.
//			builder.verify(getServiceHub());
//			final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);
//
//
//			// Step 7. Collect the other party's signature using the SignTransactionFlow.
//			List<Party> otherParties = state.getParticipants()
//					.stream().map(el -> (Party)el)
//					.collect(Collectors.toList());
//
//			otherParties.remove(getOurIdentity());
//
//			List<FlowSession> sessions = otherParties
//					.stream().map(el -> initiateFlow(el))
//					.collect(Collectors.toList());
//
//			SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));
//
//			// Step 8. Assuming no exceptions, we can now finalise the transaction
//			return subFlow(new FinalityFlow(stx, sessions));
//		}
//	}
//
//	/**
//	 * This is the flow which signs IOU issuances.
//	 * The signing is handled by the [SignTransactionFlow].
//	 */
//	@InitiatedBy(IOUIssueFlow.InitiatorFlow.class)
//	public static class ResponderFlow extends FlowLogic<SignedTransaction> {
//
//		private final FlowSession flowSession;
//		private SecureHash txWeJustSigned;
//
//		public ResponderFlow(FlowSession flowSession){
//			this.flowSession = flowSession;
//		}
//
//		@Suspendable
//		@Override
//		public SignedTransaction call() throws FlowException {
//
//			class SignTxFlow extends SignTransactionFlow {
//
//				private SignTxFlow(FlowSession flowSession, ProgressTracker progressTracker) {
//					super(flowSession, progressTracker);
//				}
//
//				@Override
//				protected void checkTransaction(SignedTransaction stx) {
//					requireThat(req -> {
//						ContractState output = stx.getTx().getOutputs().get(0).getData();
//						req.using("This must be an IOU transaction", output instanceof IOUState);
//						return null;
//					});
//					// Once the transaction has verified, initialize txWeJustSignedID variable.
//					txWeJustSigned = stx.getId();
//				}
//			}
//
//			flowSession.getCounterpartyFlowInfo().getFlowVersion();
//
//			// Create a sign transaction flow
//			SignTxFlow signTxFlow = new SignTxFlow(flowSession, SignTransactionFlow.Companion.tracker());
//
//			// Run the sign transaction flow to sign the transaction
//			subFlow(signTxFlow);
//
//			// Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
//			return subFlow(new ReceiveFinalityFlow(flowSession, txWeJustSigned));
//
//		}
//	}
//}