package val.db.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.AT;
import val.ValNetwork;
import val.at.AT_API_Helper;
import val.at.AT_Constants;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.ATStore;
import val.db.store.DerivedTableManager;
import val.schema.Tables;
import val.schema.tables.records.AtRecord;
import val.schema.tables.records.AtStateRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static val.schema.Tables.*;

public class SqlATStore implements ATStore {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);

    private final DbKey.LongKeyFactory<AT> atDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<AT>("id") {
        @Override
        public DbKey newKey(AT at) {
            return at.dbKey;
        }
    };
    private final VersionedEntityTable<AT> atTable;

    private final DbKey.LongKeyFactory<AT.ATState> atStateDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<AT.ATState>("at_id") {
        @Override
        public DbKey newKey(AT.ATState atState) {
            return atState.dbKey;
        }
    };

    private final VersionedEntityTable<AT.ATState> atStateTable;

    public SqlATStore(DerivedTableManager derivedTableManager) {
        atTable = new VersionedEntitySqlTable<AT>("at", Tables.AT, atDbKeyFactory, derivedTableManager) {
            @Override
            protected AT load(DSLContext ctx, ResultSet rs) {
                //return new AT(rs);
                throw new RuntimeException("AT attempted to be created with atTable.load");
            }

            @Override
            protected void save(DSLContext ctx, AT at) {
                saveAT(ctx, at);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("id", Long.class).asc());
                return sort;
            }
        };

        atStateTable = new VersionedEntitySqlTable<AT.ATState>("at_state", Tables.AT_STATE, atStateDbKeyFactory, derivedTableManager) {
            @Override
            protected AT.ATState load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlATState(rs);
            }

            @Override
            protected void save(DSLContext ctx, AT.ATState atState) {
                saveATState(ctx, atState);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("prev_height", Integer.class).asc());
                sort.add(tableClass.field("height", Integer.class).asc());
                sort.add(tableClass.field("at_id", Long.class).asc());
                return sort;
            }
        };
    }

    private void saveATState(DSLContext ctx, AT.ATState atState) {
        AtStateRecord atStateRecord = ctx.newRecord(Tables.AT_STATE);
        atStateRecord.setAtId(atState.getATId());
        atStateRecord.setState(val.AT.compressState(atState.getState()));
        atStateRecord.setPrevHeight(atState.getPrevHeight());
        atStateRecord.setNextHeight(atState.getNextHeight());
        atStateRecord.setSleepBetween(atState.getSleepBetween());
        atStateRecord.setPrevBalance(atState.getPrevBalance());
        atStateRecord.setFreezeWhenSameBalance(atState.getFreezeWhenSameBalance());
        atStateRecord.setMinActivateAmount(atState.getMinActivationAmount());
        atStateRecord.setHeight(ValNetwork.getBlockchain().getHeight());
        atStateRecord.setLatest(true);
        DbUtils.mergeInto(
                ctx, atStateRecord, Tables.AT_STATE,
                (new Field[]{atStateRecord.field("at_id"), atStateRecord.field("height")})
        );
    }

    private void saveAT(DSLContext ctx, AT at) {
        ctx.insertInto(
                AT,
                AT.ID, AT.CREATOR_ID, AT.NAME, AT.DESCRIPTION,
                AT.VERSION, AT.CSIZE, AT.DSIZE, AT.C_USER_STACK_BYTES,
                AT.C_CALL_STACK_BYTES, AT.CREATION_HEIGHT,
                AT.AP_CODE, AT.HEIGHT
        ).values(
                AT_API_Helper.getLong(at.getId()), AT_API_Helper.getLong(at.getCreator()), at.getName(), at.getDescription(),
                at.getVersion(), at.getCsize(), at.getDsize(), at.getC_user_stack_bytes(),
                at.getC_call_stack_bytes(), at.getCreationBlockHeight(),
                val.AT.compressState(at.getApCode()), ValNetwork.getBlockchain().getHeight()
        ).execute();
    }

    @Override
    public boolean isATAccountId(Long id) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.fetchExists(ctx.selectOne().from(AT).where(AT.ID.eq(id)).and(AT.LATEST.isTrue()));
    }

    @Override
    public List<Long> getOrderedATs() {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectFrom(
                AT.join(AT_STATE).on(AT.ID.eq(AT_STATE.AT_ID)).join(ACCOUNT).on(AT.ID.eq(ACCOUNT.ID))
        ).where(
                AT.LATEST.isTrue()
        ).and(
                AT_STATE.LATEST.isTrue()
        ).and(
                ACCOUNT.LATEST.isTrue()
        ).and(
                AT_STATE.NEXT_HEIGHT.lessOrEqual(ValNetwork.getBlockchain().getHeight() + 1)
        ).and(
                ACCOUNT.BALANCE.greaterOrEqual(
                        AT_Constants.getInstance().STEP_FEE(ValNetwork.getBlockchain().getHeight())
                                * AT_Constants.getInstance().API_STEP_MULTIPLIER(ValNetwork.getBlockchain().getHeight())
                )
        ).and(
                AT_STATE.FREEZE_WHEN_SAME_BALANCE.isFalse().or(
                        "account.balance - at_state.prev_balance >= at_state.min_activate_amount"
                )
        ).orderBy(
                AT_STATE.PREV_HEIGHT.asc(), AT_STATE.NEXT_HEIGHT.asc(), AT.ID.asc()
        ).fetch().getValues(AT.ID);
    }

    @Override
    public AT getAT(Long id) {
        DSLContext ctx = Db.getDSLContext();
        Record record = ctx.select(AT.fields()).select(AT_STATE.fields()).from(AT.join(AT_STATE).on(AT.ID.eq(AT_STATE.AT_ID))).
                where(AT.LATEST.isTrue().
                        and(AT_STATE.LATEST.isTrue()).
                        and(AT.ID.eq(id))).fetchOne();

        if (record == null) {
            return null;
        }

        AtRecord at = record.into(AT);
        AtStateRecord atState = record.into(AT_STATE);

        return createAT(at, atState);
    }

    private AT createAT(AtRecord at, AtStateRecord atState) {
        return new AT(AT_API_Helper.getByteArray(at.getId()), AT_API_Helper.getByteArray(at.getCreatorId()), at.getName(), at.getDescription(), at.getVersion(),
                val.AT.decompressState(atState.getState()), at.getCsize(), at.getDsize(), at.getCUserStackBytes(), at.getCCallStackBytes(), at.getCreationHeight(), atState.getSleepBetween(), atState.getNextHeight(),
                atState.getFreezeWhenSameBalance(), atState.getMinActivateAmount(), val.AT.decompressState(at.getApCode()));
    }

    @Override
    public List<Long> getATsIssuedBy(Long accountId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectFrom(AT).where(AT.LATEST.isTrue()).and(AT.CREATOR_ID.eq(accountId)).orderBy(AT.CREATION_HEIGHT.desc(), AT.ID.asc()).fetch().getValues(AT.ID);
    }

    @Override
    public Collection<Long> getAllATIds() {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectFrom(AT).where(AT.LATEST.isTrue()).fetch().getValues(AT.ID);
    }

    @Override
    public DbKey.LongKeyFactory<AT> getAtDbKeyFactory() {
        return atDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<AT> getAtTable() {
        return atTable;
    }

    @Override
    public DbKey.LongKeyFactory<AT.ATState> getAtStateDbKeyFactory() {
        return atStateDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<AT.ATState> getAtStateTable() {
        return atStateTable;
    }

    @Override
    public Long findTransaction(int startHeight, int endHeight, Long atID, int numOfTx, long minAmount) {
        try (DSLContext ctx = Db.getDSLContext()) {
            SelectQuery query = ctx.select(TRANSACTION.ID).from(TRANSACTION).where(
                    TRANSACTION.HEIGHT.between(startHeight, endHeight - 1)
            ).and(
                    TRANSACTION.RECIPIENT_ID.eq(atID)
            ).and(
                    TRANSACTION.AMOUNT.greaterOrEqual(minAmount)
            ).orderBy(
                    TRANSACTION.HEIGHT, TRANSACTION.ID
            ).getQuery();
            DbUtils.applyLimits(query, numOfTx, numOfTx + 1);
            try (ResultSet rs = query.fetchResultSet()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public int findTransactionHeight(Long transactionId, int height, Long atID, long minAmount) {

        DSLContext ctx = Db.getDSLContext();
        try (Cursor<Record1<Long>> cursor = ctx.select(TRANSACTION.ID)
                .from(TRANSACTION)
                .where(TRANSACTION.HEIGHT.eq(height))
                .and(TRANSACTION.RECIPIENT_ID.eq(atID))
                .and(TRANSACTION.AMOUNT.greaterOrEqual(minAmount))
                .orderBy(TRANSACTION.HEIGHT, TRANSACTION.ID)
                .fetchLazy()) {

            int counter = 0;
            while (cursor.hasNext()) {
                counter++;
                long currentTransactionId = cursor.fetchNext().getValue(TRANSACTION.ID);
                if (currentTransactionId == transactionId) {
                    break;
                }
            }
            return counter;
        } catch (DataAccessException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    class SqlATState extends AT.ATState {
        private SqlATState(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("at_id"),
                    rs.getBytes("state"),
                    rs.getInt("next_height"),
                    rs.getInt("sleep_between"),
                    rs.getLong("prev_balance"),
                    rs.getBoolean("freeze_when_same_balance"),
                    rs.getLong("min_activate_amount")
            );
        }
    }
}
