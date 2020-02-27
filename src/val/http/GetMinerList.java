package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Blockchain;
import val.ValException;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;

public final class GetMinerList extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final Blockchain blockchain;
    private final TimeService timeService;

    GetMinerList(ParameterService parameterService, AccountService accountService, Blockchain blockchain, TimeService timeService) {
        super(new APITag[]{APITag.ACCOUNTS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, ACCOUNT_PARAMETER, DUR_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
        this.blockchain = blockchain;
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        Account account = parameterService.getAccount(req);
        if (account == null) {
            return JSONResponses.INCORRECT_ACCOUNT;
        }
        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);

        final JsonObject response = new JsonObject();
        final JsonArray pledges = new JsonArray();
        int startTime = 0;
        int endTime = 0;
        String timeHourDur = Convert.emptyToNull(req.getParameter(Parameters.DUR_PARAMETER));
        if (timeHourDur != null && Convert.isNumber(timeHourDur)) { // unit hour
            endTime = timeService.getEpochTime();
            startTime = endTime - Integer.parseInt(timeHourDur) * 60 * 60;
            //System.out.printf("getBlockCount by st:%s, et:%s", startTime, endTime);
        }
        boolean isCurrentPool = false;
        try (DbIterator<? extends Account.Pledges> pledgesIterator = accountService.getPoolAllMinerPledge(account.getId(), page, limit)) {
            while (pledgesIterator.hasNext()) {
                final Account.Pledges pledge = pledgesIterator.next();
                if (pledge != null && pledge.getRecipID() == account.getId() && pledge.getLatest() == 1) {
                    isCurrentPool = true;
                }
                pledges.add(JSONData.miner(account.getId(), pledge, blockchain.getAccountBlockCount(pledge.getAccountID(), account.getId(), startTime, endTime), 0, accountService.getAccount(pledge.getAccountID()), isCurrentPool));
                isCurrentPool = false;
            }
        }

        int total = accountService.getPoolAllMinerPledgeCount(account.getId());

        return JSONData.listResponse(0, "OK", total, pledges);
    }

}
