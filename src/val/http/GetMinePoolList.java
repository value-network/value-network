package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import val.Account;
import val.Blockchain;
import val.MinePool;
import val.ValException;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.Parameters.DUR_PARAMETER;

public final class GetMinePoolList extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final Blockchain blockchain;
    private final TimeService timeService;

    GetMinePoolList(ParameterService parameterService, AccountService accountService, Blockchain blockchain, TimeService timeService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, DUR_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
        this.blockchain = blockchain;
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        final AtomicInteger total = new AtomicInteger(0);
        final JsonArray minePools = new JsonArray();
        int startTime = 0;
        int endTime = 0;
        ArrayList minePoolArr = new ArrayList<Long>();
        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));
        String timeHourDur = Convert.emptyToNull(req.getParameter(Parameters.DUR_PARAMETER));
        long accountLong = 0;
        if (accountId == null) {
            Map<Long, byte[]> poolMap = MinePool.getInstance().getMinePoolMap();
            Iterator<Map.Entry<Long, byte[]>> entries = poolMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<Long, byte[]> entry = entries.next();
                minePoolArr.add(entry.getKey());
                //System.out.println("minepool account = " + entry.getKey() + ", publicKey = " + entry.getValue());
            }
        } else {
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
            minePoolArr.add(accountLong);
        }

        if (timeHourDur != null && Convert.isNumber(timeHourDur)) { // unit hour
            endTime = timeService.getEpochTime();
            startTime = endTime - Integer.parseInt(timeHourDur) * 60 * 60;
            //System.out.printf("getBlockCount by st:%s, et:%s", startTime, endTime);
        }

        try (DbIterator<? extends Account> mineIterator = accountService.getMinePoolAccount(minePoolArr)) {
            while (mineIterator.hasNext()) {
                final Account minePool = mineIterator.next();
                total.addAndGet(1);
//					byte[] pk = MinePool.getInstance().getMinePoolMap().get(minePool.getId());
//					if (pk == null){
//						minePools.add(JSONData.minePool(minePool,accountService.getAccountPledged(),blockchain.getLatestBlockReward(minePool.getId()),0,accountService.getMinePoolMinerCount(minePool.getId())));
//					}else{
                //总收益需汇总所有矿工收益
                long minerTotalReward = accountService.getMinerTotalReward(minePool.getId());
                minePool.setForgedBalanceNQT(minePool.getForgedBalanceNQT() + minerTotalReward);
                minePools.add(JSONData.minePool(minePool, accountService.getAccountPledged(), blockchain.getLatestBlockReward(minePool.getId()), blockchain.getPoolBlockCount(minePool.getId(), startTime, endTime), accountService.getMinePoolMinerCount(minePool.getId()), MinePool.getInstance().verifyMinePooler(minePool.getId())));
                //}

            }
        }

        return JSONData.listResponse(0, "OK", total.get(), minePools);
    }

}
