package net.corda.training.flow;


import net.corda.core.contracts.UniqueIdentifier;

public class IOUTransferFlowArgs {
    private String clientId;
    private UniqueIdentifier stateLinearId;
    private String newLender;

    public IOUTransferFlowArgs(){};

    public IOUTransferFlowArgs(String clientId, UniqueIdentifier stateLinearId, String newLender){
        this.clientId = clientId;
        this.stateLinearId = stateLinearId;
        this.newLender = newLender;
    }

    public String getClientId() {
        return clientId;
    }

    public UniqueIdentifier getStateLinearId(){
        return stateLinearId;
    }

    public String getNewLender(){
        return newLender;
    }
}


