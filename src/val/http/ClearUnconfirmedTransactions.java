package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.TransactionProcessor;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.DONE_RESPONSE;
import static val.http.common.ResultFields.ERROR_RESPONSE;

public final class ClearUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    private final TransactionProcessor transactionProcessor;

    ClearUnconfirmedTransactions(TransactionProcessor transactionProcessor) {
        super(new APITag[]{APITag.DEBUG});
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        JsonObject response = new JsonObject();
        try {
            transactionProcessor.clearUnconfirmedTransactions();
            response.addProperty(DONE_RESPONSE, true);
        } catch (RuntimeException e) {
            response.addProperty(ERROR_RESPONSE, e.toString());
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
