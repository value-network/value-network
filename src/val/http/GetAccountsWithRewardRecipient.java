package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.ValException;
import val.db.DbIterator;
import val.services.AccountService;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ACCOUNTS_RESPONSE;
import static val.http.common.Parameters.ACCOUNT_PARAMETER;

public final class GetAccountsWithRewardRecipient extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;

    GetAccountsWithRewardRecipient(ParameterService parameterService, AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        JsonObject response = new JsonObject();

        Account targetAccount = parameterService.getAccount(req);

        JsonArray accounts = new JsonArray();

        DbIterator<Account.RewardRecipientAssignment> assignments = accountService.getAccountsWithRewardRecipient(targetAccount.getId());
        while (assignments.hasNext()) {
            Account.RewardRecipientAssignment assignment = assignments.next();
            accounts.add(Convert.toUnsignedLong(assignment.getAccountId()));
        }
        if (accountService.getRewardRecipientAssignment(targetAccount) == null) {
            accounts.add(Convert.toUnsignedLong(targetAccount.getId()));
        }

        response.add(ACCOUNTS_RESPONSE, accounts);

        return response;
    }
}
