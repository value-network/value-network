package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.ValException;
import val.http.common.Parameters;
import val.services.ParameterService;
import val.util.Convert;
import val.util.JSON;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.PUBLIC_KEY_RESPONSE;

public final class GetAccountPublicKey extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;

    GetAccountPublicKey(ParameterService parameterService) {
        super(new APITag[]{APITag.ACCOUNTS}, Parameters.ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        Account account = parameterService.getAccount(req);

        if (account.getPublicKey() != null) {
            JsonObject response = new JsonObject();
            response.addProperty(PUBLIC_KEY_RESPONSE, Convert.toHexString(account.getPublicKey()));
            return response;
        } else {
            return JSON.emptyJSON;
        }
    }

}
