package net.corda.training.webserver;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.training.flow.IOUIssueFlow;
import net.corda.training.flow.IOUIssueFlowArgs;
import net.corda.training.flow.IOUTransferFlow;
import net.corda.training.flow.IOUTransferFlowArgs;
import net.corda.training.states.IOUState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.ServiceUnavailableException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }

    //Implement flows using HTTP POST and receive parameters in a JSON format
    @PostMapping(value = "/iou_issue/start")
    public ResponseEntity<?> startIOUIssue(@RequestBody IOUIssueFlowArgs iouIssueFlowArgs){

        //Cant take two parties in a format of "Party", therefore,
        // take them as String objects then turn into CordaX500name and then turn into Party objects.
        CordaX500Name lenderName = CordaX500Name.parse(iouIssueFlowArgs.getLender());
        Party lender = proxy.wellKnownPartyFromX500Name(lenderName);

        CordaX500Name borrowerName = CordaX500Name.parse(iouIssueFlowArgs.getBorrower());
        Party borrower = proxy.wellKnownPartyFromX500Name(borrowerName);

        try{
            CordaFuture<SignedTransaction> result = proxy.startFlowDynamicWithClientId(
                    iouIssueFlowArgs.getClientId(),
                    IOUIssueFlow.InitiatorFlow.class,
                    iouIssueFlowArgs.getCurrency(),
                    iouIssueFlowArgs.getAmount(),
                    lender,
                    borrower
            ).getReturnValue();

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/iou_transfer/start")
    public ResponseEntity<?> startIOUTransfer(@RequestBody IOUTransferFlowArgs iouTransferFlowArgs){

        //Cant take two parties in a format of "Party", therefore,
        // take them as String objects then turn into CordaX500name and then turn into Party objects.
        CordaX500Name lenderName = CordaX500Name.parse(iouTransferFlowArgs.getNewLender());
        Party newLender = proxy.wellKnownPartyFromX500Name(lenderName);

        try{
            CordaFuture<SignedTransaction> result = proxy.startFlowDynamicWithClientId(
                    iouTransferFlowArgs.getClientId(),
                    IOUTransferFlow.InitiatorFlow.class,
                    iouTransferFlowArgs.getStateLinearId(),
                    newLender
            ).getReturnValue();

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    //Reattach to flow using clientId
//    @PostMapping(value = "/iou_issue/reattach", consumes = "application/json")
//    public ResponseEntity<String> reattachIOUIssue(@RequestParam(value = "clientId") String clientId){
//        try{
//            String result = proxy.reattachFlowWithClientId(clientId).getReturnValue().toString();
//            return ResponseEntity.status(HttpStatus.CREATED).body(result);
//        }catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(e.getMessage());
//        }
//    }

    //Delete client id
//    @PostMapping(value = "/iou_issue/remove", consumes = "application/json")
//    public ResponseEntity<?> getIOUIssue(@RequestParam(value = "clientId") String clientId){
//        try{
//            Boolean isClientIdRemoved = proxy.removeClientId(clientId);
//            return ResponseEntity.status(HttpStatus.CREATED).body(isClientIdRemoved);
//        }catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(e.getMessage());
//        }
//
//    }

    //Get transactions
//    @GetMapping(value = "/iou_issue/transaction/get", consumes = "application/json")
//    public ResponseEntity<?> getIOUissueTransaction(@RequestParam(value = "txnId") String txnId){
//        try{
//            String result = proxy.getVaultTransactionNotes(SecureHash.create(txnId)).toString();
//            return ResponseEntity.status(HttpStatus.CREATED).body(result);
//        }catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(e.getMessage());
//        }
//    }
}