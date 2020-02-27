package val.services.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import val.*;
import val.db.DbKey;
import val.db.VersionedBatchEntityTable;
import val.db.VersionedEntityTable;
import val.db.store.AccountStore;
import val.db.store.GlobalParameterStore;
import val.services.GlobalParameterService;
import val.util.Convert;
import val.util.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GlobalParameterServiceImpl implements GlobalParameterService {
    private final GlobalParameterStore globalParameterStore;
    private final VersionedEntityTable<GlobalParameter> globalParameterTable;
    private final DbKey.LongKeyFactory<GlobalParameter> globalParameterDbKeyFactory;

    private final AccountStore accountStore;
    private final VersionedBatchEntityTable<Account> accountTable;
    private final DbKey.LongKeyFactory<Account> accountDbKeyFactory;

    public GlobalParameterServiceImpl(GlobalParameterStore globalParameterStore, AccountStore accountStore) {
        this.globalParameterStore = globalParameterStore;
        this.globalParameterTable = globalParameterStore.getGlobalParameterTable();
        this.globalParameterDbKeyFactory = globalParameterStore.getGlobalParameterKeyFactory();

        this.accountStore = accountStore;
        this.accountTable = accountStore.getAccountTable();
        this.accountDbKeyFactory = accountStore.getAccountKeyFactory();
    }

    @Override
    public HashMap<Long, String> getAllGlobalParametersCache() {
        return globalParameterStore.getAllGlobalParametersCache();
    }

    @Override
    public String getPledgeRangeMinCache() {
        return globalParameterStore.getPledgeRangeMinCache();
    }

    @Override
    public String getPledgeRangeMaxCache() {
        return globalParameterStore.getPledgeRangeMaxCache();
    }

    @Override
    public String getMaxPledgeRewardCache() {
        return globalParameterStore.getMaxPledgeRewardCache();
    }

    @Override
    public String getPoolMaxCapicityCache() {
        return globalParameterStore.getPoolMaxCapicityCache();
    }

    @Override
    public String getGenBlockRatioCache() {
        return globalParameterStore.getGenBlockRatioCache();
    }

    @Override
    public String getPoolRewardPercentCache() {
        return globalParameterStore.getPoolRewardPercentCache();
    }

    @Override
    public String getMinerRewardPercentCache() {
        return globalParameterStore.getMinerRewardPercentCache();
    }

    @Override
    public String getPoolCountCache() {
        return globalParameterStore.getPoolCountCache();
    }

    @Override
    public String getPoolerAddressListCache() {
        return globalParameterStore.getPoolerAddressListCache();
    }

    @Override
    public void addOrUpdateParams(Transaction transaction, Attachment at) {
        Attachment.MessagingGlobalPrameter attachment = (Attachment.MessagingGlobalPrameter) at;
        String pledgeRangeMin = Convert.emptyToNull(attachment.getPledgeRangeMin());
        String pledgeRangeMax = Convert.emptyToNull(attachment.getPledgeRangeMax());
        String maxPledgeReward = Convert.emptyToNull(attachment.getMaxPledgeReward());
        String poolMaxCapicity = Convert.emptyToNull(attachment.getPoolMaxCapicity());
        String genBlockRatio = Convert.emptyToNull(attachment.getGenBlockRatio());
        String poolRewardPercent = Convert.emptyToNull(attachment.getPoolRewardPercent());
        String minerRewardPercent = Convert.emptyToNull(attachment.getMinerRewardPercent());
        String poolCount = Convert.emptyToNull(attachment.getPoolCount());
        String poolerAddressList = Convert.emptyToNull(attachment.getPoolerAddressList());
        if (!Convert.isNullorEmpty(pledgeRangeMin) && Convert.isNumber(pledgeRangeMin)
                && Convert.parseUnsignedLong(pledgeRangeMin) > 0) {
            setPledgeRangeMinCache(pledgeRangeMin, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(pledgeRangeMax) && Convert.isNumber(pledgeRangeMax)
                && Convert.parseUnsignedLong(pledgeRangeMax) > 0) {
            setPledgeRangeMaxCache(pledgeRangeMax, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(maxPledgeReward) && Convert.isNumber(maxPledgeReward)
                && Convert.parseUnsignedLong(maxPledgeReward) > 0) {
            setMaxPledgeRewardCache(maxPledgeReward, transaction.getId(), transaction.getHeight());
            System.out.printf("change maxPledgeReward at:%d, to:%s, transaction:%s\n", transaction.getHeight(),
                    maxPledgeReward, transaction.getJsonObject().toString());
            MinePool.getInstance().changePledgeReward((long) transaction.getHeight() + 1,
                    Long.parseLong(maxPledgeReward));
        }
        if (!Convert.isNullorEmpty(poolMaxCapicity) && Convert.isNumber(poolMaxCapicity)
                && Convert.parseUnsignedLong(poolMaxCapicity) > 0) {
            setPoolMaxCapicityCache(poolMaxCapicity, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(genBlockRatio) && Convert.isNumber(genBlockRatio)
                && Convert.parseUnsignedLong(genBlockRatio) > 0) {
            setGenBlockRatioCache(genBlockRatio, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(poolRewardPercent) && Convert.isNumber(poolRewardPercent)
                && Convert.parseUnsignedLong(poolRewardPercent) > 0) {
            setPoolRewardPercentCache(poolRewardPercent, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(minerRewardPercent) && Convert.isNumber(minerRewardPercent)
                && Convert.parseUnsignedLong(minerRewardPercent) > 0) {
            setMinerRewardPercentCache(minerRewardPercent, transaction.getId(), transaction.getHeight());
        }
        if (!Convert.isNullorEmpty(poolCount) && Convert.isNumber(poolCount)
                && Convert.parseUnsignedLong(poolCount) > 0) {
            setPoolCountCache(poolCount, transaction.getId(), transaction.getHeight());
        }
        try {
            if (!Convert.isNullorEmpty(poolerAddressList)) {
                setPoolerAddressListCache(poolerAddressList, transaction.getId(), transaction.getHeight());
            }
        } catch (RuntimeException e) {
            System.out.printf("parse json object for [poolerAddressList] error: %s", e.getMessage());
            e.printStackTrace();
        }
        // 0:userA,userB|1:userC,userD - add userA, userB into pool, remove userC, userD
        // from pool
        // if(poolerAddressList != null && Strings.split(poolerAddressList,':').length >
        // 0){
        // setPoolerAddressListCache(poolerAddressList,transaction.getId(),transaction.getHeight());
        // }
    }

    @Override
    public String setPoolCountCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_POOL_COUNT, blockHeight);
        }
        System.out.printf("setPoolCountCache, value: %s, transaction id: %s\n", value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_POOL_COUNT);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_POOL_COUNT);
        ;
        param.setValue(value);
        // System.out.print("setPledgeRangeMinCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setPledgeRangeMinCache end insert globalParameterTable
        // \n");
        globalParameterStore.setPoolCountCache(value);
        return null;
    }

    @Override
    public String setPledgeRangeMinCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_PLEDGE_RANGE_MIN, blockHeight);
        }
        System.out.printf("setPledgeRangeMinCache, value: %s, transaction id: %s\n", value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_PLEDGE_RANGE_MIN);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_PLEDGE_RANGE_MIN);
        ;
        param.setValue(value);
        // System.out.print("setPledgeRangeMinCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setPledgeRangeMinCache end insert globalParameterTable
        // \n");
        globalParameterStore.setPledgeRangeMinCache(value);
        return null;
    }

    @Override
    public String setPledgeRangeMaxCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_PLEDGE_RANGE_MAX, blockHeight);
        }
        System.out.printf("setPledgeRangeMaxCache, value: %s, transaction id: %s\n", value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_PLEDGE_RANGE_MAX);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_PLEDGE_RANGE_MAX);
        param.setValue(value);
        // System.out.print("setPledgeRangeMaxCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setPledgeRangeMaxCache end insert globalParameterTable
        // \n");
        globalParameterStore.setPledgeRangeMaxCache(value);
        return null;
    }

    @Override
    public String setMaxPledgeRewardCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_MAX_PLEDGE_REWARD, blockHeight);
        }
        // System.out.printf("setMaxPledgeRewardCache, value: %s, transaction id:
        // %s\n",value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_MAX_PLEDGE_REWARD);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_MAX_PLEDGE_REWARD);
        param.setValue(value);
        // System.out.print("setMaxPledgeRewardCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setMaxPledgeRewardCache end insert globalParameterTable
        // \n");
        globalParameterStore.setMaxPledgeRewardCache(value);
        return null;
    }

    @Override
    public String setPoolMaxCapicityCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_POOL_MAX_CAPICITY, blockHeight);
        }
        // System.out.printf("setPoolMaxCapicityCache, value: %s, transaction id:
        // %s\n",value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_POOL_MAX_CAPICITY);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_POOL_MAX_CAPICITY);
        param.setValue(value);
        // System.out.print("setPoolMaxCapicityCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setPoolMaxCapicityCache end insert globalParameterTable
        // \n");
        globalParameterStore.setPoolMaxCapicityCache(value);
        return null;
    }

    @Override
    public String setGenBlockRatioCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_GEN_BLOCK_RATIO, blockHeight);
        }
        // System.out.printf("setGenBlockRatioCache, value: %s, transaction id:
        // %s\n",value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_GEN_BLOCK_RATIO);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_GEN_BLOCK_RATIO);
        param.setValue(value);
        // System.out.print("setGenBlockRatioCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setGenBlockRatioCache end insert globalParameterTable \n");
        globalParameterStore.setGenBlockRatioCache(value);
        return null;
    }

    @Override
    public String setPoolRewardPercentCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_POOL_REWARD_PERCENT, blockHeight);
        }
        // System.out.printf("setPoolRewardPercentCache, value: %s, transaction id:
        // %s\n",value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_POOL_REWARD_PERCENT);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_POOL_REWARD_PERCENT);
        param.setValue(value);
        // System.out.print("setPoolRewardPercentCache start insert globalParameterTable
        // \n");
        globalParameterTable.insert(param);
        // System.out.print("setPoolRewardPercentCache end insert globalParameterTable
        // \n");
        globalParameterStore.setPoolRewardPercentCache(value);
        return null;
    }

    @Override
    public String setMinerRewardPercentCache(String value, long transactionId, int blockHeight) {
        GlobalParameter param = null;
        if (blockHeight > 0) {
            param = globalParameterStore.getGlobalParameterByName(Constants.GLOBAL_MINER_REWARD_PERCENT, blockHeight);
        }
        // System.out.printf("setMinerRewardPercentCache, value: %s, transaction id:
        // %s\n",value, transactionId);
        if (param == null) {
            DbKey globalParameterDBId = globalParameterDbKeyFactory.newKey(Constants.GLOBAL_MINER_REWARD_PERCENT);
            param = new GlobalParameter(globalParameterDBId, 0L, transactionId, null, blockHeight, 1);
        }
        param.setId(Constants.GLOBAL_MINER_REWARD_PERCENT);
        param.setValue(value);
        // System.out.print("setMinerRewardPercentCache start insert
        // globalParameterTable \n");
        globalParameterTable.insert(param);
        // System.out.print("setMinerRewardPercentCache end insert globalParameterTable
        // \n");
        globalParameterStore.setMinerRewardPercentCache(value);
        return null;
    }

    // {
    // "addPool":[{"account":"publicKey"},{"account":"2345"}],
    // "delPool":[{"account":"3534"},{"account":"43224"}],
    // }

    @Override
    public String setPoolerAddressListCache(String poolListJSONStr, long transactionId, int blockHeight) {
        try {
            JsonObject poolListData = JSON.getAsJsonObject(JSON.parse(poolListJSONStr));
            JsonArray poolListArr = poolListData.getAsJsonArray("addPool");
            List lists = new ArrayList<String>();
            if (poolListArr != null) {
                for (int i = 0; i < poolListArr.size(); i++) {
                    JsonObject jsonTemp = JSON.getAsJsonObject(poolListArr.get(i));
                    String pkStr = JSON.getAsString(jsonTemp.get("account"));
                    // System.out.printf("addPool: accountId - %s", account);
                    if (Convert.isNullorEmpty(pkStr)) {
                        continue;
                    }
                    addOrUpdatePoolAccount(pkStr, 1, blockHeight);
                    lists.add(pkStr);
                }
                if (lists.size() > 0) {
                    MinePool.getInstance().updateIsPool(lists, 1);
                }
            }

            JsonArray poolListDelArr = poolListData.getAsJsonArray("delPool");
            if (poolListDelArr != null) {
                lists = new ArrayList<Long>();
                for (int i = 0; i < poolListDelArr.size(); i++) {
                    JsonObject jsonTemp = JSON.getAsJsonObject(poolListDelArr.get(i));
                    String pkStr = JSON.getAsString(jsonTemp.get("account"));
                    // System.out.printf("delPool: accountId - %s", account);
                    if (Convert.isNullorEmpty(pkStr)) {
                        continue;
                    }
                    addOrUpdatePoolAccount(pkStr, 0, blockHeight);
                    lists.add((pkStr));
                }
                if (lists.size() > 0) {
                    MinePool.getInstance().updateIsPool(lists, 0);
                }
            }

        } catch (Exception e) {
            System.out.printf("set global parameter, the format for poollist err:%s\n", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addOrUpdatePoolAccount(String publicKey, int opType, int blockHeight) {
        byte[] pk = Convert.parseHexString(publicKey);
        if (pk == null) {
            // System.out.printf("addOrUpdatePoolAccount(pk:%s, op:%d, height:%d) invalid publicKey\n", publicKey, opType, blockHeight);
            return;
        }
        long accountId = AccountServiceImpl.getId(pk);
        Account acc = accountTable.get(accountDbKeyFactory.newKey(accountId));
        if (acc == null) {
            // System.out.printf("no account:%s, new it\n", accountId);
            acc = new Account(accountId);
            acc.setPublicKey(pk);
        }
        acc.setAccountRole(opType);
        // System.out.printf("add or update account_role: id:%s, role:%s\n",
        // acc.getId(), acc.getAccountRole());
        accountTable.insert(acc);
        // accountTable.finish();
    }

    @Override
    public void flushGlobalParameterTable() {
        globalParameterTable.finish();
    }

}
