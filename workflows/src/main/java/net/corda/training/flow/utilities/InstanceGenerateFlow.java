package net.corda.training.flow.utilities;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.finance.Currencies;
import net.corda.training.states.IOUState;

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */
@StartableByRPC
public class InstanceGenerateFlow extends FlowLogic<IOUState>{

    String currency;
    private long amount;
    Party lender;
    Party borrower;

    public InstanceGenerateFlow(String currency, long amount, Party lender, Party borrower) {
        this.currency = currency;
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
    }

    @Suspendable
    @Override
    public IOUState call() throws FlowException {
        switch (currency) {
            case "USD":
                // generate IOUState for USD
                return new IOUState(Currencies.DOLLARS(amount), lender, borrower);
            case "GBP":
                // generate IOUState for GBP
                return new IOUState(Currencies.POUNDS(amount), lender, borrower);
            case "CHF":
                // generate IOUState for CHF
                return new IOUState(Currencies.SWISS_FRANCS(amount), lender, borrower);
            default:
                // Any currency other than the above will result in an error.
                throw new FlowException("Incorrect value for currency.Please set \"USD\", \"GBP\", or \"CHF\".");
        }
    }
}
