package val.db.sql;

import org.jooq.*;
import org.slf4j.LoggerFactory;
import val.Account;
import val.Account.PoolMiner;
import val.db.store.PoolMinerStore;
import val.schema.tables.records.PoolMinerRecord;

import java.sql.ResultSet;
import java.sql.SQLException;

import static val.schema.Tables.ACCOUNT;
import static val.schema.Tables.POOL_MINER;

public class SqlPoolMinerStore implements PoolMinerStore {

    //private final VersionedEntityTable<Account.PoolMiner> poolMinerTable;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlPoolMinerStore.class);
//  private static final DbKey.LongKeyFactory<Account.PoolMiner> poolMinerKeyFactory = new DbKey.LongKeyFactory<Account.PoolMiner>("account_id") {
//      @Override
//      public DbKey newKey(Account.PoolMiner poolMiner) {
//        return (DbKey) poolMiner.dbKey;
//      }
//    };

//  public SqlPoolMinerStore(DerivedTableManager derivedTableManager) {
//	  poolMinerTable = new VersionedEntitySqlTable<Account.PoolMiner>("pool_miner", Tables.POOL_MINER, poolMinerKeyFactory, derivedTableManager) {
//
//      @Override
//      protected Account.PoolMiner load(DSLContext ctx, ResultSet rs) throws SQLException {
//        return new SqlPoolMiner(rs);
//      }
//
//      @Override
//      protected void save(DSLContext ctx, Account.PoolMiner poolMiner) {
//    	  PoolMinerRecord poolMinerRecord = ctx.newRecord(Tables.POOL_MINER);
//    	  poolMinerRecord.setAccountId(poolMiner.getAccountID());
//    	  poolMinerRecord.setPoolId(poolMiner.getPoolID());
//    	  poolMinerRecord.setStatus(poolMiner.getStatus());
//    	  poolMinerRecord.setCTime(poolMiner.getcTime());
//    	  poolMinerRecord.setMTime(poolMiner.getmTime());
//        DbUtils.mergeInto(
//            ctx, poolMinerRecord, Tables.POOL_MINER,
//            ( new Field[] { poolMinerRecord.field("account_id") } )
//        );
//      }
//
//   };
//  }
//
//
//  @Override
//  public VersionedEntityTable<Account.PoolMiner> getPoolMinerTable() {
//    return poolMinerTable;
//  }

    @Override
    public Result<Record6<Integer, Long, Long, Integer, Long, Long>> getGrantPoolMiners(long poolId, int page, int limit) {
        DSLContext ctx = Db.getDSLContext();
        SortField sortFieldBySize = (ACCOUNT.FORGED_BALANCE.add(ACCOUNT.PLEDGE_REWARD_BALANCE)).desc();

        SelectQuery selectQuery = ctx
                .select(POOL_MINER.DB_ID, POOL_MINER.ACCOUNT_ID, POOL_MINER.POOL_ID, POOL_MINER.STATUS, POOL_MINER.C_TIME, POOL_MINER.M_TIME).from(POOL_MINER).leftJoin(ACCOUNT)
                .on(ACCOUNT.ID.eq(POOL_MINER.ACCOUNT_ID).and(ACCOUNT.LATEST.eq(true))).where(
                        POOL_MINER.POOL_ID.eq(poolId).and(POOL_MINER.STATUS.eq(0))
                ).orderBy(sortFieldBySize).getQuery();
        DbUtils.applyPages(selectQuery, page, limit);

        return selectQuery.fetch();
    }

//@Override
//public DbKey.LongKeyFactory<Account.PoolMiner> getPoolMinerKeyFactory() {
//	return poolMinerKeyFactory;
//}

    @Override
    public void savePoolMiner(Account.PoolMiner poolMiner) {
        DSLContext ctx = Db.getDSLContext();
        ctx.insertInto(POOL_MINER, POOL_MINER.ACCOUNT_ID, POOL_MINER.POOL_ID, POOL_MINER.STATUS, POOL_MINER.C_TIME).values(
                poolMiner.getAccountID(), poolMiner.getPoolID(), poolMiner.getStatus(), poolMiner.getcTime()).execute();
        return;
    }

    @Override
    public void revokePoolMiner(long accountId, long poolId, int status, long mTime) {
        DSLContext ctx = Db.getDSLContext();
        ctx.update(POOL_MINER).set(POOL_MINER.STATUS, status).set(POOL_MINER.M_TIME, mTime).where(
                POOL_MINER.ACCOUNT_ID.eq(accountId).and(POOL_MINER.POOL_ID.eq(poolId))).execute();
        return;

    }

    @Override
    public int getGrantPoolMinerCount(long poolId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectCount().from(POOL_MINER).where(POOL_MINER.POOL_ID.eq(poolId).and(POOL_MINER.STATUS.eq(0))).fetchOne(0, int.class);
    }

    @Override
    public Account.PoolMiner getGrantPoolMiner(long accountId, long poolId) {
        Condition condition = POOL_MINER.ACCOUNT_ID.eq(accountId);
        condition = condition.and(POOL_MINER.STATUS.eq(0));
        if (poolId != 0) {
            condition = condition.and(POOL_MINER.POOL_ID.eq(poolId));
        }
        DSLContext ctx = Db.getDSLContext();
        PoolMinerRecord results = ctx.selectFrom(POOL_MINER).where(condition).fetchAny();
        if (results == null) {
            return null;
        }
        int db_id = results.getDbId();
        long account = results.getAccountId();
        long pool = results.getPoolId();
        int status = results.getStatus();
        long cTime = results.getCTime();
        long mTime = results.getMTime();
        Account.PoolMiner poolMiner = new PoolMiner(db_id, account, pool, status, cTime, mTime);

        return poolMiner;
    }

    class SqlPoolMiner extends Account.PoolMiner {
        SqlPoolMiner(ResultSet rs) throws SQLException {
            super(
                    rs.getInt("db_id"),
                    rs.getLong("account_id"),
                    rs.getLong("pool_id"),
                    rs.getInt("status"),
                    rs.getLong("c_time"),
                    rs.getLong("m_time")
            );
        }
    }


}
