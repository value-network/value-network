package val.db.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.slf4j.LoggerFactory;
import val.Account;
import val.Account.Pledges;
import val.db.DbIterator;
import val.db.VersionedEntityTable;
import val.db.store.DerivedTableManager;
import val.db.store.PledgeStore;
import val.schema.Tables;
import val.schema.tables.records.PledgesRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static val.schema.Tables.ACCOUNT;
import static val.schema.Tables.PLEDGES;

public class SqlPledgeStore implements PledgeStore {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SqlPledgeStore.class);
    private static final DbKey.LongKeyFactory<Account.Pledges> pledgeKeyFactory = new DbKey.LongKeyFactory<Account.Pledges>("account_id") {
        @Override
        public DbKey newKey(Account.Pledges pledge) {
            return (DbKey) pledge.dbKey;
        }
    };
    private final VersionedEntityTable<Account.Pledges> pledgeTable;

    public SqlPledgeStore(DerivedTableManager derivedTableManager) {
        pledgeTable = new VersionedEntitySqlTable<Account.Pledges>("pledges", Tables.PLEDGES, pledgeKeyFactory, derivedTableManager) {

            @Override
            protected Account.Pledges load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlPledge(rs);
            }

            @Override
            protected void save(DSLContext ctx, Account.Pledges pledge) {
                PledgesRecord pledgeRecord = ctx.newRecord(Tables.PLEDGES);
                pledgeRecord.setId(pledge.getId());
                pledgeRecord.setAccountId(pledge.getAccountID());
                pledgeRecord.setRecipId(pledge.getRecipID());
                pledgeRecord.setPledgeTotal(pledge.getPledgeTotal());
                pledgeRecord.setPledgeLatestTime(pledge.getPledgeLatestTime());
                pledgeRecord.setUnpledgeTotal(pledge.getUnpledgeTotal());
                pledgeRecord.setWithdrawTime(pledge.getWithdrawTime());
                pledgeRecord.setHeight(pledge.getHeight());
                pledgeRecord.setLatest(true);
                DbUtils.mergeInto(
                        ctx, pledgeRecord, Tables.PLEDGES,
                        (new Field[]{pledgeRecord.field("account_id"), pledgeRecord.field("recip_id"), pledgeRecord.field("height")})
                );
            }

        };
    }

    private static Condition getAccountPledgeClause(final long accountId, final long recipientId) {
        return PLEDGES.ACCOUNT_ID.eq(accountId).and(PLEDGES.RECIP_ID.eq(recipientId));
    }

    @Override
    public VersionedEntityTable<Account.Pledges> getPledgesTable() {
        return pledgeTable;
    }

    @Override
    public Account.Pledges getAccountPledge(long accountId, long recipientId) {
        return getPledgesTable().getBy(getAccountPledgeClause(accountId, recipientId));
    }


    @Override
    public Account.Pledges getAccountPledge(long accountId) {
        return getPledgesTable().getBy(PLEDGES.ACCOUNT_ID.eq(accountId));
    }

    @Override
    public Account.Pledges getAccountPledgeOne(long accountId, long totalPledged) {
        return getPledgesTable().getBy(PLEDGES.ACCOUNT_ID.eq(accountId).and(PLEDGES.PLEDGE_TOTAL.ne(totalPledged)));
    }

    @Override
    public Account.Pledges getAccountPledge(long accountId, int height) {
        if (height < 0) {
            return getAccountPledge(accountId);
        }
        return getPledgesTable().getBy(PLEDGES.ACCOUNT_ID.eq(accountId), height);
    }

    @Override
    public long getMinerTotalReward(long accountId) {
        DSLContext ctx = Db.getDSLContext();
        long forgedBalance = 0;
        long pledgeBalance = 0;
        try {
            SelectQuery selectQuery = ctx
                    .select(DSL.nvl(DSL.sum(ACCOUNT.FORGED_BALANCE), 0).as("forged_balance"), DSL.nvl(DSL.sum(ACCOUNT.PLEDGE_REWARD_BALANCE), 0).as("pledge_balance"))
                    .from(ACCOUNT)
                    .where(
                            ACCOUNT.LATEST.eq(true).and(
                                    ACCOUNT.ID.in(
                                            ctx.select(PLEDGES.ACCOUNT_ID).
                                                    from(PLEDGES).
                                                    where(PLEDGES.RECIP_ID.eq(accountId).and(PLEDGES.ACCOUNT_ID.ne(PLEDGES.RECIP_ID).and(PLEDGES.LATEST.eq(true)))))
                            )
                    ).getQuery();

            Result<Record> sqlRet = selectQuery.fetch();

            for (Record rec : sqlRet) {
                forgedBalance = new Long(rec.get("forged_balance").toString());
                pledgeBalance = new Long(rec.get("pledge_balance").toString());
            }
            System.out.printf("getMinerTotalReward for pool:%s-forgedBalance:[%s], pledgeBalance:[%s]\n", accountId, forgedBalance, pledgeBalance);
        } catch (DataAccessException e) {
            System.out.printf("getMinerTotalReward failed:%s\n%s\n", e.getMessage(), e.getStackTrace().toString());
        }
        return forgedBalance + pledgeBalance;
    }

    @Override
    public DbIterator<Pledges> getPoolAllMinerPledge(long accountId) {
        DSLContext ctx = Db.getDSLContext();

        SelectQuery selectQuery = ctx
                .selectFrom(PLEDGES).where(
                        PLEDGES.RECIP_ID.eq(accountId)
                ).and(PLEDGES.LATEST.eq(true)).getQuery();
        // DbUtils.applyLimits(selectQuery, from, to);

        return getPledgesTable().getManyBy(ctx, selectQuery, false);
    }

    @Override
    public DbIterator<Pledges> getPledges(long accountId, int page, int limit) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        conditions.add(PLEDGES.LATEST.eq(true));
        conditions.add(PLEDGES.ACCOUNT_ID.ne(0L));
        conditions.add(PLEDGES.PLEDGE_TOTAL.gt(0L));
        if (accountId != 0) {
            conditions.add(PLEDGES.ACCOUNT_ID.eq(accountId));
        }
        SelectQuery selectQuery = ctx.selectFrom(PLEDGES).where(conditions).getQuery();
        DbUtils.applyPages(selectQuery, page, limit);
        return getPledgesTable().getManyBy(ctx, selectQuery, false);
    }

    @Override
    public int getPledgesCount(long accountId) {
        DSLContext ctx = Db.getDSLContext();
        ArrayList<Condition> conditions = new ArrayList<>();
        conditions.add(PLEDGES.LATEST.eq(true));
        conditions.add(PLEDGES.ACCOUNT_ID.ne(0L));
        conditions.add(PLEDGES.PLEDGE_TOTAL.gt(0L));
        if (accountId != 0) {
            conditions.add(PLEDGES.ACCOUNT_ID.eq(accountId));
        }
        return ctx.selectCount().from(PLEDGES).where(conditions).fetchOne(0, int.class);
    }


    @Override
    public DbIterator<Pledges> getPoolAllMinerPledge(long accountId, int page, int limit) {
        DSLContext ctx = Db.getDSLContext();
        SortField sortFieldBySize = (ACCOUNT.FORGED_BALANCE.add(ACCOUNT.PLEDGE_REWARD_BALANCE)).desc();
    /*SelectQuery selectQuery = ctx
      .select(PLEDGES.ACCOUNT_ID, PLEDGES.DB_ID, PLEDGES.ID, PLEDGES.RECIP_ID,PLEDGES.PLEDGE_TOTAL,PLEDGES.PLEDGE_LATEST_TIME,PLEDGES.UNPLEDGE_TOTAL,PLEDGES.WITHDRAW_TIME, PLEDGES.HEIGHT, DSL.max(PLEDGES.LATEST).as("latest")).from(PLEDGES).leftJoin(ACCOUNT)
      .on(ACCOUNT.ID.eq(PLEDGES.ACCOUNT_ID).and(ACCOUNT.LATEST.eq(true))).where(
        PLEDGES.RECIP_ID.eq(accountId)
        ).groupBy(PLEDGES.ACCOUNT_ID).orderBy(sortFieldBySize).getQuery();*/
        SelectQuery selectQuery = ctx.select(PLEDGES.ACCOUNT_ID, PLEDGES.DB_ID, PLEDGES.ID, PLEDGES.RECIP_ID, PLEDGES.PLEDGE_TOTAL, PLEDGES.PLEDGE_LATEST_TIME, PLEDGES.UNPLEDGE_TOTAL, PLEDGES.WITHDRAW_TIME, PLEDGES.HEIGHT, PLEDGES.LATEST).from(PLEDGES)
                .leftJoin(ACCOUNT).on(ACCOUNT.ID.eq(PLEDGES.ACCOUNT_ID).and(ACCOUNT.LATEST.eq(true))).where(
                        PLEDGES.RECIP_ID.eq(accountId).and(PLEDGES.LATEST.eq(true)).and(PLEDGES.HEIGHT.in(ctx.select(DSL.max(PLEDGES.HEIGHT)).from(PLEDGES).groupBy(PLEDGES.ACCOUNT_ID)))
                ).orderBy(sortFieldBySize).getQuery();
        DbUtils.applyPages(selectQuery, page, limit);

        return getPledgesTable().getManyBy(ctx, selectQuery, false);
    }

    @Override
    public int getPoolAllMinerPledgeCount(long accountId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectCount().from(PLEDGES).where(
                PLEDGES.RECIP_ID.eq(accountId)
        ).and(PLEDGES.LATEST.eq(true)).fetchOne(0, int.class);
    }

    @Override
    public long getBlockchainPledged() {
        try (DSLContext ctx = Db.getDSLContext()) {
            return ctx.select(PLEDGES.PLEDGE_TOTAL.sum()).from(PLEDGES).where(PLEDGES.PLEDGE_TOTAL.gt(0L).and(PLEDGES.LATEST.eq(true))).fetchOne(0, long.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public DbKey.LongKeyFactory<Account.Pledges> getPledgeKeyFactory() {
        return pledgeKeyFactory;
    }

    class SqlPledge extends Account.Pledges {
        SqlPledge(ResultSet rs) throws SQLException {
            super(
                    pledgeKeyFactory.newKey(rs.getLong("account_id")),
                    rs.getLong("id"),
                    rs.getLong("account_id"),
                    rs.getLong("recip_id"),
                    rs.getLong("pledge_total"),
                    rs.getLong("pledge_latest_time"),
                    rs.getLong("unpledge_total"),
                    rs.getLong("withdraw_time"),
                    (int) rs.getLong("height"),
                    (int) rs.getLong("latest")
            );
        }
    }


}
