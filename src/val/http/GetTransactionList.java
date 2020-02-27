package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import val.*;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;

final class GetTransactionList extends APIServlet.APIRequestHandler {

    private final TransactionProcessor transactionProcessor;
    private final Blockchain blockchain;
    private final ParameterService parameterService;

    GetTransactionList(TransactionProcessor transactionProcessor, ParameterService parameterService, Blockchain blockchain) {
        super(new APITag[]{APITag.TRANSACTIONS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, TRANSACTION_PARAMETER, FULL_HASH_PARAMETER, ACCOUNT_PARAMETER,
                BLOCK_PARAMETER, HEIGHT_PARAMETER, TYPE_PARAMETER, SUBTYPE_PARAMETER);
        this.transactionProcessor = transactionProcessor;
        this.parameterService = parameterService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long transactionId = 0;
        long blockID = 0;
        byte type;
        byte subtype;
        Account account = null;
        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));
        if (accountId != null) {
            account = parameterService.getAccount(req);
        }

        String transactionIdString = Convert.emptyToNull(req.getParameter(TRANSACTION_PARAMETER));
        String transactionFullHash = Convert.emptyToNull(req.getParameter(FULL_HASH_PARAMETER));
        String blockIDString = Convert.emptyToNull(req.getParameter(BLOCK_PARAMETER));
//	    if(blockIDString != null){
//	    	blockID = Convert.parseUnsignedLong(blockIDString);
//	    }
        if (blockIDString != null) {
            if (blockIDString.startsWith("-")) {
                try {
                    blockID = Long.parseLong(blockIDString);
                } catch (Exception e) {
                    e.printStackTrace();
                    blockID = 0;
                }
            } else {
                blockID = Convert.parseUnsignedLong(blockIDString);
            }
        }
        String strHeight = Convert.emptyToNull(req.getParameter(Parameters.HEIGHT_PARAMETER));
        int height = -1;
        if (null != strHeight) {
            height = Convert.parseInt(strHeight);
        }
//	    if (transactionIdString != null ) {
//	    	transactionId = Convert.parseUnsignedLong(transactionIdString);
//	    }
        if (transactionIdString != null) {
            if (transactionIdString.startsWith("-")) {
                try {
                    transactionId = Long.parseLong(transactionIdString);
                } catch (Exception e) {
                    e.printStackTrace();
                    blockID = 0;
                }
            } else {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
            }

        }

        try {
            type = Byte.parseByte(req.getParameter(TYPE_PARAMETER));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter(SUBTYPE_PARAMETER));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);

        JsonArray transactions = new JsonArray();
        try (DbIterator<? extends Transaction> iterator = blockchain.getTransactionLists(account,
                type, subtype, transactionId, transactionFullHash, blockID, height, page, limit)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction, blockchain.getHeight()));
            }
        }
        int total = blockchain.getTransactionCount(account,
                type, subtype, transactionId, transactionFullHash, blockID, height);
        return JSONData.listResponse(0, "OK", total, transactions);

    }

}
