package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jooq.Record14;
import org.jooq.Result;
import val.Account;
import val.Blockchain;
import val.ValException;
import val.http.common.Parameters;
import val.services.AccountService;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.*;
import static val.schema.Tables.ACCOUNT;
import static val.schema.Tables.PLEDGES;

public final class GetAccountList extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final Blockchain blockchain;

    GetAccountList(ParameterService parameterService, AccountService accountService, Blockchain blockchain) {
        super(new APITag[]{APITag.ACCOUNTS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, ACCOUNT_PARAMETER,
                INCLUDE_TRANSACTIONS_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));
        boolean includeTransactions = Parameters.isTrue(req.getParameter(Parameters.INCLUDE_TRANSACTIONS_PARAMETER));

        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);

        Account account = null;
        Account.Pledges accountPledge = null;
        JsonElement resp = null;
        long accountLong = 0;
        Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> fullRecord = null;

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
        fullRecord = accountService.getAccountPledges(accountLong, page, limit);
        if (fullRecord == null) {
            System.out.printf("no account found: %s", accountLong);
            return JSONResponses.INCORRECT_ACCOUNT;
        }
        JsonArray respArr = new JsonArray();
        JsonObject response = new JsonObject();
        for (Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long> entries : fullRecord) {
//			System.out.printf(
//					"id:{%s}，balance:{%s}，unconfirmed_balance:{%s}，forged_balance:{%s}，name:{%s}，pk:{%s}, desc:{%s}, poolID:{%s}，pledge_total:{%s}，unpledge_total:{%s}，totalBalance:{%s}",
//					entries.getValue(ACCOUNT.ID), entries.getValue(ACCOUNT.BALANCE),
//					entries.getValue(ACCOUNT.UNCONFIRMED_BALANCE), entries.getValue(ACCOUNT.FORGED_BALANCE),
//					entries.getValue(ACCOUNT.NAME), entries.getValue(ACCOUNT.PUBLIC_KEY),
//					entries.getValue(ACCOUNT.DESCRIPTION), entries.getValue(PLEDGES.RECIP_ID),
//					entries.getValue(PLEDGES.PLEDGE_TOTAL), entries.getValue(PLEDGES.UNPLEDGE_TOTAL),
//					entries.get("totalBalance"));

            response = JSONData.accountBalance(entries.getValue(ACCOUNT.BALANCE),
                    entries.getValue(ACCOUNT.UNCONFIRMED_BALANCE), entries.getValue(ACCOUNT.FORGED_BALANCE), entries.getValue(ACCOUNT.PLEDGE_REWARD_BALANCE));
            JSONData.putAccount(response, ACCOUNT_RESPONSE, entries.getValue(ACCOUNT.ID));
            response.addProperty(PUBLIC_KEY_RESPONSE, entries.getValue(ACCOUNT.PUBLIC_KEY) == null ? ""
                    : Convert.toHexString(entries.getValue(ACCOUNT.PUBLIC_KEY)));

            response.addProperty(NAME_RESPONSE,
                    entries.getValue(ACCOUNT.NAME) == "" ? "" : entries.getValue(ACCOUNT.NAME));

            response.addProperty(DESCRIPTION_RESPONSE, entries.getValue(ACCOUNT.DESCRIPTION));
            response.addProperty(ACCOUNT_ROLE_RESPONSE, entries.getValue(ACCOUNT.ACCOUNT_ROLE));
            String pledgeTotal = entries.getValue(PLEDGES.PLEDGE_TOTAL) != null ? String.valueOf(entries.getValue(PLEDGES.PLEDGE_TOTAL)) : "0";
            response.addProperty(TOTAL_PLEDGE_RESPONSE, pledgeTotal);
            response.addProperty(TOTAL_UNPLEDGE_RESPONSE, entries.getValue(PLEDGES.UNPLEDGE_TOTAL) != null ? String.valueOf(entries.getValue(PLEDGES.UNPLEDGE_TOTAL)) : "0");
            if ("0".equals(pledgeTotal)) {
                JSONData.putAccount(response, MINE_POOL_RESPONSE, 0);
            } else {
                JSONData.putAccount(response, MINE_POOL_RESPONSE, entries.getValue(PLEDGES.RECIP_ID) == null ? 0 : entries.getValue(PLEDGES.RECIP_ID));
            }

            response.addProperty(TOTAL_BALANCE_RESPONSE, entries.getValue("totalBalance") == null ? "0"
                    : String.valueOf((Long) entries.getValue("totalBalance")));
            response.addProperty(WITHDRAW_TIME_RESPONSE, entries.getValue(PLEDGES.WITHDRAW_TIME));
            response.addProperty(TOTAL_BLOCK_RESPONSE, blockchain.getAccountBlockCount(entries.getValue(ACCOUNT.ID), 0, 0, 0));
            if (includeTransactions) {
                int totalTnx = blockchain.getTransactionCount(entries.getValue(ACCOUNT.ID), 3);
                int outTnx = blockchain.getTransactionCount(entries.getValue(ACCOUNT.ID), 0);
                response.addProperty(TOTAL_TRANSACTION_RESPONSE, totalTnx);
                response.addProperty(TOTAL_TRANSACTION_IN_RESPONSE, totalTnx - outTnx);
                response.addProperty(TOTAL_TRANSACTION_OUT_RESPONSE, outTnx);
            }
            respArr.add(response);
        }
        int total = accountService.getAccountPledgesCount(Convert.parseAccountId(accountId));

        return JSONData.listResponse(0, "OK", total, respArr);
    }

}
