package net.corda.training.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.AbstractParty;
import com.google.common.collect.ImmutableList;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.training.contracts.IOUContract;

import java.util.Currency;
import java.util.List;


/**
 * This is where you'll add the definition of your state object. Look at the unit tests in [IOUStateTests] for
 * instructions on how to complete the [IOUState] class.
 *
 */
@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState {

    @ConstructorForDeserialization
    private IOUState(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid, UniqueIdentifier linearId) {
    }

    public IOUState() {
    }

    /**
     *  This method will return a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
//    /**
//     * Helper methods for when building transactions for settling and transferring IOUs.
//     * - [pay] adds an amount to the paid property. It does no validation.
//     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
//     * - [copy] creates a copy of the state using the internal copy constructor ensuring the LinearId is preserved.
//     */
//    public IOUState pay(Amount<Currency> amountToPay) {
//        Amount<Currency> newAmountPaid = this.paid.plus(amountToPay);
//        return new IOUState(amount, lender, borrower, newAmountPaid, linearId);
//    }
//    public IOUState copy(Amount<Currency> amount, Party lender, Party borrower, Amount<Currency> paid) {
//        return new IOUState(amount, lender, borrower, paid, this.getLinearId());
//    }
}