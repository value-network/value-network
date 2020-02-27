package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jooq.Record6;
import org.jooq.Result;
import val.Account;
import val.Blockchain;
import val.ValException;
import val.http.common.Parameters;
import val.services.AccountService;
import val.services.TimeService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.schema.Tables.POOL_MINER;

public final class GetPoolMinerList extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final AccountService accountService;
    private final TimeService timeService;

    GetPoolMinerList(AccountService accountService, Blockchain blockchain, TimeService timeService) {
        super(new APITag[]{APITag.ACCOUNTS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, ACCOUNT_PARAMETER, DUR_PARAMETER);
        this.accountService = accountService;
        this.blockchain = blockchain;
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));

        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);

        JsonElement resp = null;
        Result<Record6<Integer, Long, Long, Integer, Long, Long>> fullRecord = null;

        fullRecord = accountService.getGrantPoolMiners(Convert.parseAccountId(accountId), page, limit);
        if (fullRecord == null) {
            System.out.printf("no account found: %s", Convert.parseAccountId(accountId));
            return JSONResponses.INCORRECT_ACCOUNT;
        }
        JsonArray respArr = new JsonArray();
        int startTime = 0;
        int endTime = 0;
        String timeHourDur = Convert.emptyToNull(req.getParameter(Parameters.DUR_PARAMETER));
        if (timeHourDur != null && Convert.isNumber(timeHourDur)) { // unit hour
            endTime = timeService.getEpochTime();
            startTime = endTime - Integer.parseInt(timeHourDur) * 60 * 60;
            //System.out.printf("getBlockCount by st:%s, et:%s", startTime, endTime);
        }
        boolean isCurrentPool = false;
        for (Record6<Integer, Long, Long, Integer, Long, Long> entries : fullRecord) {
            long minerId = entries.getValue(POOL_MINER.ACCOUNT_ID);
            Account.Pledges pledge = accountService.getAccountPledge(minerId);
            if (pledge != null && pledge.getRecipID() == Convert.parseAccountId(accountId) && pledge.getLatest() == 1) {
                isCurrentPool = true;
            }
            respArr.add(JSONData.miner(minerId, pledge, blockchain.getAccountBlockCount(minerId, Convert.parseAccountId(accountId), startTime, endTime), 0, accountService.getAccount(minerId), isCurrentPool));
            isCurrentPool = false;
        }
        int total = accountService.getGrantPoolMinerCount(Convert.parseAccountId(accountId));
        resp = JSONData.listResponse(0, "OK", total, respArr);
        return resp;
    }

}
