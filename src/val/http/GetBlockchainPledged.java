package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.ValException;
import val.services.AccountService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.TOTAL_PLEDGE_RESPONSE;


public final class GetBlockchainPledged extends APIServlet.APIRequestHandler {

    private final AccountService accountService;

    GetBlockchainPledged(AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS});
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        JsonObject resp = new JsonObject();
        resp.addProperty(TOTAL_PLEDGE_RESPONSE, accountService.getAccountPledged());
        return resp;
    }

}
