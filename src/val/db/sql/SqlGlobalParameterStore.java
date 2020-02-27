package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.slf4j.LoggerFactory;
import val.Account;
import val.Constants;
import val.GlobalParameter;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.VersionedEntityTable;
import val.db.store.DerivedTableManager;
import val.db.store.GlobalParameterStore;
import val.schema.Tables;
import val.schema.tables.records.GlobalParameterRecord;
import val.util.Convert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static val.schema.Tables.GLOBAL_PARAMETER;

public class SqlGlobalParameterStore implements GlobalParameterStore {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlGlobalParameterStore.class);
    private static final DbKey.LongKeyFactory<GlobalParameter> globalParameterKeyFactory = new DbKey.LongKeyFactory<GlobalParameter>("id") {
        @Override
        public DbKey newKey(GlobalParameter params) {
            return (DbKey) params.dbKey;
        }
    };
    private final VersionedEntityTable<GlobalParameter> globalParameterTable;
    private final HashMap<Long, String> paramsDataStore;

    public SqlGlobalParameterStore(DerivedTableManager derivedTableManager) {
        globalParameterTable = new VersionedEntitySqlTable<GlobalParameter>("global_parameter", Tables.GLOBAL_PARAMETER, globalParameterKeyFactory, derivedTableManager) {

            @Override
            protected GlobalParameter load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlGlobalParameter(rs);
            }

            @Override
            protected void save(DSLContext ctx, GlobalParameter params) {
                GlobalParameterRecord globalParameterRecord = ctx.newRecord(Tables.GLOBAL_PARAMETER);
                globalParameterRecord.setId(params.getId());
                globalParameterRecord.setTransactionId(params.getTransactionId());
                globalParameterRecord.setValue(params.getValue());
                globalParameterRecord.setHeight(ValNetwork.getBlockchain().getHeight() + 1);
                globalParameterRecord.setLatest(true);
                DbUtils.mergeInto(
                        ctx, globalParameterRecord, Tables.GLOBAL_PARAMETER,
                        (new Field[]{globalParameterRecord.field("id"), globalParameterRecord.field("height")})
                );
            }

        };
        paramsDataStore = new HashMap<>();
//    System.out.printf("start get globalParameter data\n");
//    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    Runnable loadAllGlobalParameters= () -> {
//      synchronized (paramsDataStore) {
//    	  try{
//    		  BurstIterator<GlobalParameter> paramsIterator = getAllGlobalParameters();
//    		  while (paramsIterator.hasNext()) {
//    			  GlobalParameter param = paramsIterator.next();
//    			  System.out.printf("get globalParameter data, name:[%s], value:[%s], height:[%s]\n", param.getId(),param.getValue(),param.getHeight());
//    			  if (Constants.GLOBAL_PLEDGE_RANGE_MIN == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MIN, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_PLEDGE_RANGE_MAX == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MAX, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_MAX_PLEDGE_REWARD == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_MAX_PLEDGE_REWARD, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_POOL_MAX_CAPICITY == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_POOL_MAX_CAPICITY, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_GEN_BLOCK_RATIO == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_GEN_BLOCK_RATIO, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_POOL_REWARD_PERCENT == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_POOL_REWARD_PERCENT, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_MINER_REWARD_PERCENT == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_MINER_REWARD_PERCENT, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_POOLER_ADDRESS_LIST == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_POOLER_ADDRESS_LIST, param.getValue());
//    			  }
//    			  if (Constants.GLOBAL_POOL_COUNT == param.getId()){
//    				  paramsDataStore.put(Constants.GLOBAL_POOL_COUNT, param.getValue());
//    			  }
//    			  
//    		  }
//    	  }catch(Exception e){
//    		  e.printStackTrace();
//    	  }
//      }
//    };
//    scheduler.scheduleWithFixedDelay(loadAllGlobalParameters, 0, 24, TimeUnit.HOURS);
    }

    @Override
    public HashMap<Long, String> getAllGlobalParametersCache() {
        synchronized (paramsDataStore) {
            HashMap<Long, String> data = paramsDataStore;
            return data;
        }
    }

    @Override
    public String getPledgeRangeMinCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_PLEDGE_RANGE_MIN, String.valueOf(Constants.GLOBAL_PLEDGE_RANGE_MIN_DEFAULT));
        }
    }

    @Override
    public String getPledgeRangeMaxCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_PLEDGE_RANGE_MAX, String.valueOf(Constants.GLOBAL_PLEDGE_RANGE_MAX_DEFAULT));
        }
    }

    @Override
    public String getMaxPledgeRewardCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_MAX_PLEDGE_REWARD, String.valueOf(Constants.GLOBAL_MAX_PLEDGE_REWARD_DEFAULT));
        }
    }

    @Override
    public String getPoolMaxCapicityCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_POOL_MAX_CAPICITY, String.valueOf(Constants.GLOBAL_POOL_MAX_CAPICITY_DEFAULT));
        }
    }

    @Override
    public String getGenBlockRatioCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_GEN_BLOCK_RATIO, String.valueOf(Constants.GLOBAL_GEN_BLOCK_RATIO_DEFAULT));
        }
    }

    @Override
    public String getPoolRewardPercentCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_POOL_REWARD_PERCENT, String.valueOf(Constants.GLOBAL_POOL_REWARD_PERCENT_DEFAULT));
        }
    }

    @Override
    public String getMinerRewardPercentCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_MINER_REWARD_PERCENT, String.valueOf(Constants.GLOBAL_MINER_REWARD_PERCENT_DEFAULT));
        }
    }

    @Override
    public String getPoolerAddressListCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_POOLER_ADDRESS_LIST, Convert.rsAccount(Account.getId(Convert.parseHexString(Constants.FOUNDATION_PUBLIC_KEY_HEX))));
        }
    }

    @Override
    public String getPoolCountCache() {
        synchronized (paramsDataStore) {
            return paramsDataStore.getOrDefault(Constants.GLOBAL_POOL_COUNT, String.valueOf(Constants.GLOBAL_POOL_COUNT_DEFAULT));
        }
    }

    @Override
    public String setPledgeRangeMinCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MIN, value);
        }
    }

    @Override
    public String setPoolCountCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_POOL_COUNT, value);
        }
    }

    @Override
    public String setPledgeRangeMaxCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MAX, value);
        }
    }

    @Override
    public String setMaxPledgeRewardCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_MAX_PLEDGE_REWARD, value);
        }
    }

    @Override
    public String setPoolMaxCapicityCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_POOL_MAX_CAPICITY, value);
        }
    }

    @Override
    public String setGenBlockRatioCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_GEN_BLOCK_RATIO, value);
        }
    }

    @Override
    public String setPoolRewardPercentCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_POOL_REWARD_PERCENT, value);
        }
    }

    @Override
    public String setMinerRewardPercentCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_MINER_REWARD_PERCENT, value);
        }
    }

    @Override
    public String setPoolerAddressListCache(String value) {
        synchronized (paramsDataStore) {
            return paramsDataStore.put(Constants.GLOBAL_POOLER_ADDRESS_LIST, value);
        }
    }

    @Override
    public VersionedEntityTable<GlobalParameter> getGlobalParameterTable() {
        return globalParameterTable;
    }

    @Override
    public DbKey.LongKeyFactory<GlobalParameter> getGlobalParameterKeyFactory() {
        return globalParameterKeyFactory;
    }

    @Override
    public DbIterator<GlobalParameter> getAllGlobalParameters() {
//		DSLContext ctx = Db.getDSLContext();
//
//		SelectQuery selectQuery = ctx.selectFrom(GLOBAL_PARAMETER).where(GLOBAL_PARAMETER.LATEST.eq(true)).getQuery();
//
//		return getGlobalParameterTable().getManyBy(ctx, selectQuery, false);

        synchronized (paramsDataStore) {
            try {
                DSLContext ctx = Db.getDSLContext();
                SelectQuery selectQuery = ctx.selectFrom(GLOBAL_PARAMETER).where(GLOBAL_PARAMETER.LATEST.eq(true)).getQuery();
                DbIterator<GlobalParameter> paramsIterator = getGlobalParameterTable().getManyBy(ctx, selectQuery, false);
                while (paramsIterator.hasNext()) {
                    GlobalParameter param = paramsIterator.next();
//	    			  System.out.printf("get globalParameter data, name:[%s], value:[%s], height:[%s]\n", param.getId(),param.getValue(),param.getHeight());
                    if (Constants.GLOBAL_PLEDGE_RANGE_MIN == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MIN, param.getValue());
                    }
                    if (Constants.GLOBAL_PLEDGE_RANGE_MAX == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_PLEDGE_RANGE_MAX, param.getValue());
                    }
                    if (Constants.GLOBAL_MAX_PLEDGE_REWARD == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_MAX_PLEDGE_REWARD, param.getValue());
                    }
                    if (Constants.GLOBAL_POOL_MAX_CAPICITY == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_POOL_MAX_CAPICITY, param.getValue());
                    }
                    if (Constants.GLOBAL_GEN_BLOCK_RATIO == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_GEN_BLOCK_RATIO, param.getValue());
                    }
                    if (Constants.GLOBAL_POOL_REWARD_PERCENT == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_POOL_REWARD_PERCENT, param.getValue());
                    }
                    if (Constants.GLOBAL_MINER_REWARD_PERCENT == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_MINER_REWARD_PERCENT, param.getValue());
                    }
                    if (Constants.GLOBAL_POOLER_ADDRESS_LIST == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_POOLER_ADDRESS_LIST, param.getValue());
                    }
                    if (Constants.GLOBAL_POOL_COUNT == param.getId()) {
                        paramsDataStore.put(Constants.GLOBAL_POOL_COUNT, param.getValue());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public GlobalParameter getGlobalParameterByName(long name, int height) {
        return getGlobalParameterTable().getBy(GLOBAL_PARAMETER.ID.eq(name));
    }

    class SqlGlobalParameter extends GlobalParameter {
        SqlGlobalParameter(ResultSet rs) throws SQLException {
            super(
                    globalParameterKeyFactory.newKey(rs.getLong("id")),
                    rs.getLong("id"),
                    rs.getLong("transaction_id"),
                    rs.getString("value"),
                    (int) rs.getLong("height"),
                    (int) rs.getLong("latest")
            );
        }
    }

}
