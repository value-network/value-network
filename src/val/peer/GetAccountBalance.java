package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.services.AccountService;
import val.util.Convert;
import val.util.JSON;

public class GetAccountBalance extends PeerServlet.PeerRequestHandler {

    static final String ACCOUNT_ID_PARAMETER_FIELD = "account";
    static final String BALANCE_NQT_RESPONSE_FIELD = "balanceNQT";
    private final AccountService accountService;

    GetAccountBalance(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(JsonObject request, Peer peer) {

        JsonObject response = new JsonObject();

        try {
            Long accountId = Convert.parseAccountId(JSON.getAsString(request.get(ACCOUNT_ID_PARAMETER_FIELD)));
            Account account = accountService.getAccount(accountId);
            if (account != null) {
                response.addProperty(BALANCE_NQT_RESPONSE_FIELD, Convert.toUnsignedLong(account.getBalanceNQT()));
            } else {
                response.addProperty(BALANCE_NQT_RESPONSE_FIELD, "0");
            }
        } catch (Exception e) {
        }

        return response;
    }
}
