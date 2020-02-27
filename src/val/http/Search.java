package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bouncycastle.util.Strings;
import val.Account;
import val.Blockchain;
import val.Transaction;
import val.http.common.Parameters;
import val.services.AccountService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static val.http.common.Parameters.SEARCH_PARAMETER;

final class Search extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final AccountService accountService;

    Search(Blockchain blockchain, AccountService accountService) {
        super(new APITag[]{APITag.BLOCKS}, SEARCH_PARAMETER);
        this.blockchain = blockchain;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        String search = Convert.emptyToNull(req.getParameter(Parameters.SEARCH_PARAMETER));
        if (search == null) {
            return JSONResponses.ERROR_INCORRECT_REQUEST;
        }
        JsonObject response = new JsonObject();
        JsonArray labels = new JsonArray();
        try {
            // search blockid
            List<Long> ids = blockchain.getBlockByLikeId(search);
            JsonArray values = new JsonArray();
            for (Long id : ids) {
                values.add(Convert.toUnsignedLong(id));
            }
            if (values.size() > 0) {
                JsonObject blocks = new JsonObject();
                blocks.addProperty("label", "Block");
                blocks.add("values", values);
                labels.add(blocks);
            }

            // search transaction id
            List<Long> tids = blockchain.getTransactionByLikeId(search);
            values = new JsonArray();
            for (Long id : tids) {
                values.add(Convert.toUnsignedLong(id));
            }
            if (values.size() > 0) {
                JsonObject transactionObj = new JsonObject();
                transactionObj.addProperty("label", "Transaction ID");
                transactionObj.add("values", values);
                labels.add(transactionObj);
            }

            // search account
            List<Long> accounts = accountService.getAccountByLikeId(search);
            values = new JsonArray();
            for (Long id : accounts) {
                values.add(Convert.toUnsignedLong(id));
            }
            if (Strings.toUpperCase(search.trim()).startsWith("VAL")) {
                long accountId = Convert.parseAccountId(search.trim());
                Account acc = accountService.getAccount(accountId);
                //System.out.printf("search:%s, accountId:%s,acc:%s", search, accountId,acc);
                if (acc != null) {
                    values.add(Convert.toUnsignedLong(accountId));
                }
            }
            if (values.size() > 0) {
                JsonObject accountObj = new JsonObject();
                accountObj.addProperty("label", "Account");
                accountObj.add("values", values);
                labels.add(accountObj);
            }

            List<Long> minePools = accountService.getPoolAccountByLikeId(search);
            values = new JsonArray();
            for (Long id : minePools) {
                values.add(Convert.toUnsignedLong(id));
            }
            if (values.size() > 0) {
                JsonObject minePoolObj = new JsonObject();
                minePoolObj.addProperty("label", "Mining Pool");
                minePoolObj.add("values", values);
                labels.add(minePoolObj);
            }
            List<Transaction> transactions = blockchain.getTransactionByLikeFullHash(search);
            JsonArray hashvalues = new JsonArray();
            for (Transaction transaction : transactions) {
                hashvalues.add(String.valueOf(transaction.getFullHash()));
            }
            if (hashvalues.size() > 0) {
                JsonObject tnxHash = new JsonObject();
                tnxHash.addProperty("label", "Transaction Hash");
                tnxHash.add("values", hashvalues);
                labels.add(tnxHash);
            }

            response.add("options", labels);
        } catch (Exception e) {
            e.printStackTrace();
            response.add("options", labels);
            return response;
        }
        return response;
    }

}
