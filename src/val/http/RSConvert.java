package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_ACCOUNT;
import static val.http.JSONResponses.MISSING_ACCOUNT;
import static val.http.common.Parameters.ACCOUNT_PARAMETER;

final class RSConvert extends APIServlet.APIRequestHandler {

    static final RSConvert instance = new RSConvert();

    private RSConvert() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.UTILS}, ACCOUNT_PARAMETER);
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        String accountValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
        if (accountValue == null) {
            return MISSING_ACCOUNT;
        }
        try {
            long accountId = Convert.parseAccountId(accountValue);
            if (accountId == 0) {
                return INCORRECT_ACCOUNT;
            }
            JsonObject response = new JsonObject();
            JSONData.putAccount(response, "account", accountId);
            return response;
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }
    }

}
