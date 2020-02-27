package val.db.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.LoggerFactory;
import val.Account;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.VersionedBatchEntityTable;
import val.db.VersionedEntityTable;
import val.db.cache.DBCacheManagerImpl;
import val.db.store.AccountStore;
import val.db.store.DerivedTableManager;
import val.schema.Tables;
import val.schema.tables.records.AccountRecord;
import val.util.Convert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static val.schema.Tables.*;

public class SqlAccountStore implements AccountStore {

    private static final DbKey.LongKeyFactory<Account> accountDbKeyFactory = new DbKey.LongKeyFactory<Account>("id") {
        @Override
        public DbKey newKey(Account account) {
            return (DbKey) account.nxtKey;
        }
    };
    private static final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentDbKeyFactory
            = new DbKey.LongKeyFactory<Account.RewardRecipientAssignment>("account_id") {
        @Override
        public DbKey newKey(Account.RewardRecipientAssignment assignment) {
            return (DbKey) assignment.dbKey;
        }
    };
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlAccountStore.class);
    private static final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetDbKeyFactory
            = new DbKey.LinkKeyFactory<Account.AccountAsset>("account_id", "asset_id") {
        @Override
        public DbKey newKey(Account.AccountAsset accountAsset) {
            return (DbKey) accountAsset.dbKey;
        }
    };
    private final VersionedEntityTable<Account.AccountAsset> accountAssetTable;
    private final VersionedEntityTable<Account.RewardRecipientAssignment> rewardRecipientAssignmentTable;
    private final VersionedBatchEntityTable<Account> accountTable;

    public SqlAccountStore(DerivedTableManager derivedTableManager, DBCacheManagerImpl dbCacheManager) {
        rewardRecipientAssignmentTable = null;
        accountAssetTable = null;
        // rewardRecipientAssignmentTable = new VersionedEntitySqlTable<Account.RewardRecipientAssignment>("reward_recip_assign", Tables.REWARD_RECIP_ASSIGN, rewardRecipientAssignmentDbKeyFactory, derivedTableManager) {

        //   @Override
        //   protected Account.RewardRecipientAssignment load(DSLContext ctx, ResultSet rs) throws SQLException {
        //     return new SqlRewardRecipientAssignment(rs);
        //   }

        //   @Override
        //   protected void save(DSLContext ctx, Account.RewardRecipientAssignment assignment) {
        //     RewardRecipAssignRecord rewardRecord = ctx.newRecord(Tables.REWARD_RECIP_ASSIGN);
        //     rewardRecord.setAccountId(assignment.accountId);
        //     rewardRecord.setPrevRecipId(assignment.getPrevRecipientId());
        //     rewardRecord.setRecipId(assignment.getRecipientId());
        //     rewardRecord.setFromHeight(assignment.getFromHeight());
        //     rewardRecord.setHeight(Volume.getBlockchain().getHeight());
        //     rewardRecord.setLatest(true);
        //     DbUtils.mergeInto(
        //         ctx, rewardRecord, Tables.REWARD_RECIP_ASSIGN,
        //         ( new Field[] { rewardRecord.field("account_id"), rewardRecord.field("height") } )
        //     );
        //   }
        // };

        // accountAssetTable = new VersionedEntitySqlTable<Account.AccountAsset>("account_asset", Tables.ACCOUNT_ASSET, accountAssetDbKeyFactory, derivedTableManager) {

        //   @Override
        //   protected Account.AccountAsset load(DSLContext ctx, ResultSet rs) throws SQLException {
        //     return new SQLAccountAsset(rs);
        //   }

        //   @Override
        //   protected void save(DSLContext ctx, Account.AccountAsset accountAsset) {
        //     AccountAssetRecord assetRecord = ctx.newRecord(Tables.ACCOUNT_ASSET);
        //     assetRecord.setAccountId(accountAsset.accountId);
        //     assetRecord.setAssetId(accountAsset.assetId);
        //     assetRecord.setQuantity(accountAsset.getQuantityQNT());
        //     assetRecord.setUnconfirmedQuantity(accountAsset.getUnconfirmedQuantityQNT());
        //     assetRecord.setHeight(Volume.getBlockchain().getHeight());
        //     assetRecord.setLatest(true);
        //     DbUtils.mergeInto(
        //         ctx, assetRecord, Tables.ACCOUNT_ASSET,
        //         ( new Field[] { assetRecord.field("account_id"), assetRecord.field("asset_id"), assetRecord.field("height") } )
        //     );
        //   }

        //   @Override
        //   protected List<SortField> defaultSort() {
        //     List<SortField> sort = new ArrayList<>();
        //     sort.add(tableClass.field("quantity", Long.class).desc());
        //     sort.add(tableClass.field("account_id", Long.class).asc());
        //     sort.add(tableClass.field("asset_id", Long.class).asc());
        //     return sort;
        //   }

        // };

        accountTable = new VersionedBatchEntitySqlTable<Account>("account", Tables.ACCOUNT, accountDbKeyFactory, derivedTableManager, dbCacheManager) {
            @Override
            protected Account load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlAccount(rs);
            }

            @Override
            protected void bulkInsert(DSLContext ctx, ArrayList<Account> accounts) {
                ArrayList<Query> accountQueries = new ArrayList<>();
                for (Account account : accounts) {
                    accountQueries.add(
                            ctx.mergeInto(ACCOUNT, ACCOUNT.ID, ACCOUNT.HEIGHT, ACCOUNT.CREATION_HEIGHT, ACCOUNT.PUBLIC_KEY, ACCOUNT.KEY_HEIGHT, ACCOUNT.BALANCE,
                                    ACCOUNT.UNCONFIRMED_BALANCE, ACCOUNT.FORGED_BALANCE, ACCOUNT.NAME, ACCOUNT.DESCRIPTION, ACCOUNT.LATEST, ACCOUNT.TOTAL_PLEDGED, ACCOUNT.ACCOUNT_ROLE, ACCOUNT.PLEDGE_REWARD_BALANCE)
                                    .key(ACCOUNT.ID, ACCOUNT.HEIGHT).values(account.getId(), ValNetwork.getBlockchain().getHeight(), account.getCreationHeight(), account.getPublicKey(), account.getKeyHeight(),
                                    account.getBalanceNQT(), account.getUnconfirmedBalanceNQT(), account.getForgedBalanceNQT(), account.getName(), account.getDescription(), true,
                                    account.getTotalPledged(), account.getAccountRole(), account.getPledgeRewardBalance())
                    );
                }
                ctx.batch(accountQueries).execute();
            }

            @Override
            public void fillCache(ArrayList<Long> ids) {
                try (DSLContext ctx = Db.getDSLContext()) {
                    try (Cursor<AccountRecord> cursor = ctx.selectFrom(Tables.ACCOUNT).where(
                            Tables.ACCOUNT.LATEST.isTrue()
                    ).and(
                            Tables.ACCOUNT.ID
                                    .in(ids.stream().distinct().collect(Collectors.toList()))
                    ).fetchLazy()) {

                        while (cursor.hasNext()) {
                            AccountRecord account = cursor.fetchNext();
                            try {
                                DbKey dbKey = (DbKey) accountDbKeyFactory.newKey(account.getId());
                                getCache().put(dbKey, new SqlAccount(account.intoResultSet()));
                            } catch (SQLException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        };
    }

    private static Condition getAccountsWithRewardRecipientClause(final long id, final int height) {
        return REWARD_RECIP_ASSIGN.RECIP_ID.eq(id).and(REWARD_RECIP_ASSIGN.FROM_HEIGHT.le(height));
    }

    @Override
    public VersionedBatchEntityTable<Account> getAccountTable() {
        return accountTable;
    }

    @Override
    public VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable() {
        return rewardRecipientAssignmentTable;
    }

    @Override
    public DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory() {
        return rewardRecipientAssignmentDbKeyFactory;
    }

    @Override
    public DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory() {
        return accountAssetDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Account.AccountAsset> getAccountAssetTable() {
        return accountAssetTable;
    }

    @Override
    public int getAssetAccountsCount(long assetId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectCount().from(ACCOUNT_ASSET).where(ACCOUNT_ASSET.ASSET_ID.eq(assetId)).and(ACCOUNT_ASSET.LATEST.isTrue()).fetchOne(0, int.class);
    }

    @Override
    public DbKey.LongKeyFactory<Account> getAccountKeyFactory() {
        return accountDbKeyFactory;
    }

    @Override
    public DbIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
        return getRewardRecipientAssignmentTable().getManyBy(getAccountsWithRewardRecipientClause(recipientId, ValNetwork.getBlockchain().getHeight() + 1), 0, -1);
    }

    @Override
    public DbIterator<Account.AccountAsset> getAssets(int from, int to, Long id) {
        return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ACCOUNT_ID.eq(id), from, to);
    }

    @Override
    public Result<AccountRecord> getAccountRoles(int roleCode, int from, int to) {
        DSLContext ctx = Db.getDSLContext();
        SelectQuery selectQuery = ctx.selectFrom(ACCOUNT).where(
                ACCOUNT.ACCOUNT_ROLE.eq(roleCode)
        ).and(ACCOUNT.LATEST.eq(true)).getQuery();

        return selectQuery.fetch();
//    return getAccountTable().getManyBy(ACCOUNT.ACCOUNT_ROLE.eq(roleCode), from, to);
    }


    @Override
    public int getAccountPledgesCount(long accountId) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        conditions.add(ACCOUNT.LATEST.eq(true));
        conditions.add(ACCOUNT.ID.ne(0L));
        if (accountId != 0) {
            conditions.add(ACCOUNT.ID.eq(accountId));
        }
        return ctx.selectCount()
                .from(ACCOUNT).leftOuterJoin(PLEDGES)
                .on(ACCOUNT.ID.eq(PLEDGES.ACCOUNT_ID).and(PLEDGES.LATEST.eq(true)))
                .where(conditions).fetchOne(0, int.class);
    }

    public List<Long> getTotalBalance() {
        DSLContext ctx = Db.getDSLContext();

        List<Long> ret = new ArrayList();

        try {
            SelectQuery selectQuery = ctx
                    .select(DSL.nvl(DSL.sum(ACCOUNT.BALANCE), 0).as("balance"), DSL.nvl(DSL.sum(ACCOUNT.UNCONFIRMED_BALANCE), 0).as("unconfirm_balance"), DSL.nvl(DSL.sum(ACCOUNT.FORGED_BALANCE), 0).as("forge_balance"))
                    .from(ACCOUNT)
                    .where(ACCOUNT.LATEST.eq(true)).getQuery();

            Result<Record> sqlRet = selectQuery.fetch();

            for (Record rec : sqlRet) {
                ret.add(new Long(rec.get("balance").toString()));
                ret.add(new Long(rec.get("unconfirm_balance").toString()));
                ret.add(new Long(rec.get("forge_balance").toString()));
            }
        } catch (DataAccessException e) {
            System.out.printf("getTotalBalance failed:%s\n%s\n", e.getMessage(), e.getStackTrace().toString());
        }

        return ret;
    }

    public List<Long> getTotalPledge() {
        DSLContext ctx = Db.getDSLContext();

        List<Long> ret = new ArrayList();

        try {
            SelectQuery selectQuery = ctx
                    .select(DSL.nvl(DSL.sum(PLEDGES.PLEDGE_TOTAL), 0).as("pledge_total"), DSL.nvl(DSL.sum(PLEDGES.UNPLEDGE_TOTAL), 0).as("unpledge_total"))
                    .from(PLEDGES)
                    .where(PLEDGES.LATEST.eq(true)).getQuery();

            Result<Record> sqlRet = selectQuery.fetch();
            for (Record rec : sqlRet) {
                ret.add(new Long(rec.get("pledge_total").toString()));
                ret.add(new Long(rec.get("unpledge_total").toString()));
            }
        } catch (DataAccessException e) {
            System.out.printf("getTotalPledge failed:%s\n%s\n", e.getMessage(), e.getStackTrace().toString());
        }

        return ret;
    }

    @Override
    public Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> getAccountPledges(long accountId, int page, int limit) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        conditions.add(ACCOUNT.LATEST.eq(true));
        conditions.add(ACCOUNT.ID.ne(0L));
        if (accountId != 0) {
            conditions.add(ACCOUNT.ID.eq(accountId));
        }

        SortField sortFieldBySize = (ACCOUNT.BALANCE.add(PLEDGES.PLEDGE_TOTAL).add(PLEDGES.UNPLEDGE_TOTAL)).desc();

        SelectQuery selectQuery = ctx.select(ACCOUNT.ID, ACCOUNT.BALANCE, ACCOUNT.UNCONFIRMED_BALANCE, ACCOUNT.FORGED_BALANCE,
                ACCOUNT.NAME, ACCOUNT.PUBLIC_KEY, ACCOUNT.DESCRIPTION, PLEDGES.RECIP_ID, PLEDGES.PLEDGE_TOTAL, PLEDGES.UNPLEDGE_TOTAL,
                PLEDGES.WITHDRAW_TIME, ACCOUNT.ACCOUNT_ROLE, ACCOUNT.PLEDGE_REWARD_BALANCE,
                DSL.ifnull(ACCOUNT.BALANCE, 0).add(DSL.ifnull(PLEDGES.PLEDGE_TOTAL, 0)).add(DSL.ifnull(PLEDGES.UNPLEDGE_TOTAL, 0)).as("totalBalance"))
                .from(ACCOUNT).leftJoin(PLEDGES)
                .on(ACCOUNT.ID.eq(PLEDGES.ACCOUNT_ID).and(PLEDGES.LATEST.eq(true)))
                .where(conditions).orderBy(sortFieldBySize).getQuery();
        DbUtils.applyPages(selectQuery, page, limit);

        Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> results = selectQuery.fetch();

//	    for (Record11<Long,Long,Long,Long,String,Byte[],String,Long,Long,Long,Long> record:results){
//	        System.out.printf("id:{%s}，balance:{%s}，unconfirmed_balance:{%s}，forged_balance:{%s}，name:{%s}，poolID:{%s}，pledge_total:{%s}，unpledge_total:{%s}，totalBalance:{%s}", 
//	        record.getValue(ACCOUNT.ID),record.getValue(ACCOUNT.BALANCE),record.getValue(ACCOUNT.UNCONFIRMED_BALANCE),
//	        record.getValue(ACCOUNT.FORGED_BALANCE),record.getValue(ACCOUNT.NAME),record.getValue(PLEDGES.RECIP_ID),
//	        record.getValue(PLEDGES.PLEDGE_TOTAL),record.getValue(PLEDGES.UNPLEDGE_TOTAL),record.getValue("totalBalance"));
//	    }
        return results;
    }


    @Override
    public DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        List<SortField> sort = new ArrayList<>();
        sort.add(ACCOUNT_ASSET.field("quantity", Long.class).desc());
        sort.add(ACCOUNT_ASSET.field("account_id", Long.class).asc());
        return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId), from, to, sort);
    }

    @Override
    public DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }

        List<SortField> sort = new ArrayList<>();
        sort.add(ACCOUNT_ASSET.field("quantity", Long.class).desc());
        sort.add(ACCOUNT_ASSET.field("account_id", Long.class).asc());
        return getAccountAssetTable().getManyBy(ACCOUNT_ASSET.ASSET_ID.eq(assetId), height, from, to, sort);
    }

    @Override
    public boolean setOrVerify(Account acc, byte[] key, int height) {
        if (acc.getPublicKey() == null) {
            if (Db.isInTransaction()) {
                acc.setPublicKey(key);
                acc.setKeyHeight(-1);
                getAccountTable().insert(acc);
            }
            return true;
        } else if (Arrays.equals(acc.getPublicKey(), key)) {
            return true;
        } else if (acc.getKeyHeight() == -1) {
            logger.info("DUPLICATE KEY!!!");
            logger.info("Account key for " + Convert.toUnsignedLong(acc.id) + " was already set to a different one at the same height "
                    + ", current height is " + height + ", rejecting new key");
            return false;
        } else if (acc.getKeyHeight() >= height) {
            logger.info("DUPLICATE KEY!!!");
            if (Db.isInTransaction()) {
                logger.info("Changing key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                        + ", was previously set to a different one at height " + acc.getKeyHeight());
                acc.setPublicKey(key);
                acc.setKeyHeight(height);
                getAccountTable().insert(acc);
            }
            return true;
        }
        logger.info("DUPLICATE KEY!!!");
        logger.info("Invalid key for account " + Convert.toUnsignedLong(acc.id) + " at height " + height
                + ", was already set to a different one at height " + acc.getKeyHeight());
        return false;
    }

    static class SQLAccountAsset extends Account.AccountAsset {
        SQLAccountAsset(ResultSet rs) throws SQLException {
            super(rs.getLong("account_id"),
                    rs.getLong("asset_id"),
                    rs.getLong("quantity"),
                    rs.getLong("unconfirmed_quantity"),
                    accountAssetDbKeyFactory.newKey(rs.getLong("account_id"), rs.getLong("asset_id"))
            );
        }
    }

    class SqlAccount extends Account {
        SqlAccount(Long id) {
            super(id);
        }

        SqlAccount(ResultSet rs) throws SQLException {
            super(rs.getLong("id"), accountDbKeyFactory.newKey(rs.getLong("id")),
                    rs.getInt("creation_height"));
            this.setPublicKey(rs.getBytes("public_key"));
            this.setKeyHeight(rs.getInt("key_height"));
            this.balanceNQT = rs.getLong("balance");
            this.unconfirmedBalanceNQT = rs.getLong("unconfirmed_balance");
            this.forgedBalanceNQT = rs.getLong("forged_balance");
            this.totalPledged = rs.getLong("total_pledged");
            this.accountRole = rs.getInt("account_role");
            this.name = rs.getString("name");
            this.description = rs.getString("description");
            this.pledgeRewardBalance = rs.getLong("pledge_reward_balance");
        }
    }

    class SqlRewardRecipientAssignment extends Account.RewardRecipientAssignment {
        SqlRewardRecipientAssignment(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("account_id"),
                    rs.getLong("prev_recip_id"),
                    rs.getLong("recip_id"),
                    (int) rs.getLong("from_height"),
                    rewardRecipientAssignmentDbKeyFactory.newKey(rs.getLong("account_id"))
            );
        }
    }


}
