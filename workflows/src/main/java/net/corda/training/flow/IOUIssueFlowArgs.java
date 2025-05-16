package net.corda.training.flow;

import net.corda.core.identity.Party;

public class IOUIssueFlowArgs {
    private String clientId;
    private String currency;
    private long amount;
    private String lender;
    private String borrower;

    public IOUIssueFlowArgs(){}

    public IOUIssueFlowArgs(String clientId, String currency, long amount, String lender, String borrower){
        this.clientId = clientId;
        this.currency = currency;
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
    }
    public String getClientId(){
        return  clientId;
    }

    public String getCurrency(){
        return currency;
    }

    public long getAmount() {
        return amount;
    }

    public String getLender(){
        return lender;
    }

    public String getBorrower() {
        return borrower;
    }

}
