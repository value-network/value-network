package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import val.Account;
import val.ValException;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.AccountService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;

public final class GetPledges extends APIServlet.APIRequestHandler {

    private final AccountService accountService;

    GetPledges(AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, ACCOUNT_PARAMETER);
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));

        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);

        Account.Pledges accountPledge = null;
        JsonElement resp = null;
        long accountLong = 0;
        DbIterator<Account.Pledges> fullRecord = null;

        // if (accountId == null) {
        if (accountId != null) {
            if (accountId.startsWith("-")) {
                try {
                    accountLong = Long.parseLong(accountId);
                } catch (Exception e) {
                    e.printStackTrace();
                    accountLong = 0;
                }
            } else {
                accountLong = Convert.parseAccountId(accountId);
            }
        }

        JsonArray pledges = new JsonArray();
        try (DbIterator<? extends Account.Pledges> iterator = accountService.getPledges(accountLong, page, limit)) {
            while (iterator.hasNext()) {
                Account.Pledges pledge = iterator.next();
                pledges.add(JSONData.pledges(pledge));
            }
        }
        int total = accountService.getPledgesCount(accountLong);

        return JSONData.listResponse(0, "OK", total, pledges);

    }

}
