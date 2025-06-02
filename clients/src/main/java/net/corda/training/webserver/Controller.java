package net.corda.training.webserver;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.FlowHandleWithClientId;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.training.flow.IOUIssueFlow;
import net.corda.training.flow.IOUTransferFlow;
import net.corda.training.states.IOUState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.ServiceUnavailableException;
import java.util.*;
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

    // Implement IOUIssueFlow as HTTP POST request.
//    @PostMapping(value = "/iou/issue")
//    public ResponseEntity<?> startIOUIssue(@RequestBody IOUIssueFlowArgs iouIssueFlowArgs){
//
//        // Cant take two parties as Party objects, therefore,
//        // First, take these as String objects from IOUIssueFlowArgs,
//        // then convert them to CordaX500Name, and finally to Party objects.
//        CordaX500Name lenderName = CordaX500Name.parse(iouIssueFlowArgs.getLender());
//        Party lender = proxy.wellKnownPartyFromX500Name(lenderName);
//
//        CordaX500Name borrowerName = CordaX500Name.parse(iouIssueFlowArgs.getBorrower());
//        Party borrower = proxy.wellKnownPartyFromX500Name(borrowerName);
//
//        // Start IOU issue flow, using startFlowDynamicWithClientId.
//        try{
//            FlowHandleWithClientId result_iou_issue = proxy.startFlowDynamicWithClientId(
//                    iouIssueFlowArgs.getClientId(),
//                    IOUIssueFlow.InitiatorFlow.class,
//                    iouIssueFlowArgs.getCurrency(),
//                    iouIssueFlowArgs.getAmount(),
//                    lender,
//                    borrower
//            );
//
//            CordaFuture<SignedTransaction> sampleFuture = result_iou_issue.getReturnValue();
//            SignedTransaction stx = sampleFuture.get();
//
//            // Extract every parameter from FlowHandleWithClientId and put them into Map for output of the flow result.
//            // For those who want to see the details of IOUIssueFlow, including results, pls run VaultQuery or refer logs.
//            // This POST method cannot return the flow results as seen in the Corda node shell,
//            // since startFlowDynamicWithClientId only returns a FlowHandleWithClientId object.
//            Map<String,String> mapped_result_iou_issue = new HashMap<>();
//            mapped_result_iou_issue.put("clientId",result_iou_issue.getClientId());
//            mapped_result_iou_issue.put("StateMachineId", String.valueOf(result_iou_issue.getId()));
//            mapped_result_iou_issue.put("SignedTransaction", stx.toString());
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(mapped_result_iou_issue);
//
//        } catch (Exception e) {
//            // Errors which might occur between clients and servers will be caught here.
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }

//    // Implement IOUTransferFlow as HTTP POST request.
//    @PostMapping(value = "/iou/transfer")
//    public ResponseEntity<?> startIOUTransfer(@RequestBody IOUTransferFlowArgs iouTransferFlowArgs){
//
//    }

    //Capture all states.
//    @GetMapping(value = "/iou_state/all")
//    public ResponseEntity<?> vaultQueryAllIOUState(){
//
//        // Define query criteria that filters all IOU states.
//        QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
//
//        try{
//
//            Vault.Page<IOUState> results = proxy.vaultQueryByCriteria(criteria, IOUState.class);
//
//            // Instantiate a list to store IOU state's info.
//            List<Map<String,String>> result_for_output = new ArrayList<>();
//
//            // Put the desired information into the map and repeat the process
//            // until all IOU states have been processed.
//            results.getStates().forEach(iouStateStateAndRef -> {
//
//                // Create Map object and put entries for result_for_output
//                Map<String,String> entry = new HashMap<>();
//                entry.put("amount",iouStateStateAndRef.getState().getData().getAmount().toString());
//                entry.put("lender",iouStateStateAndRef.getState().getData().getLender().toString());
//                entry.put("borrower",iouStateStateAndRef.getState().getData().getBorrower().toString());
//                entry.put("linearId",iouStateStateAndRef.getState().getData().getLinearId().getId().toString());
//                entry.put("txnId",iouStateStateAndRef.getRef().getTxhash().toString());
//
//                result_for_output.add(entry);
//
//            });
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(result_for_output);
//
//        } catch (Exception e) {
//            // Included a catch block to handle potential HTTP request errors.
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }

//    // Capture unconsumed states
//    @GetMapping(value = "/iou_state/unconsumed")
//    public ResponseEntity<?> vaultQueryUnconsumedIOUState(){
//
//    }

    // Capture consumed states
//    @GetMapping(value = "/iou_state/consumed")
//    public ResponseEntity<?> vaultQueryConsumedIOUState(){
//
//    }
}