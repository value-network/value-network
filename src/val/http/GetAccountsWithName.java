package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.ValException;
import val.db.DbIterator;
import val.services.AccountService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ACCOUNTS_RESPONSE;
import static val.http.common.Parameters.NAME_PARAMETER;

public class GetAccountsWithName extends APIServlet.APIRequestHandler {

    private final AccountService accountService;

    GetAccountsWithName(AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS}, NAME_PARAMETER);
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest request) throws ValException {
        DbIterator<Account> accounts = accountService.getAccountsWithName(request.getParameter(NAME_PARAMETER));
        JsonArray accountIds = new JsonArray();

        while (accounts.hasNext()) {
            accountIds.add(Convert.toUnsignedLong(accounts.next().id));
        }

        accounts.close();

        JsonObject response = new JsonObject();
        response.add(ACCOUNTS_RESPONSE, accountIds);
        return response;
    }
}
