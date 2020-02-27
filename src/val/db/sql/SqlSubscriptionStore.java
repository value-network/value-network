package val.db.sql;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import val.Subscription;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.DerivedTableManager;
import val.db.store.SubscriptionStore;
import val.schema.Tables;
import val.schema.tables.records.SubscriptionRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static val.schema.Tables.SUBSCRIPTION;

public class SqlSubscriptionStore implements SubscriptionStore {

    private final DbKey.LongKeyFactory<Subscription> subscriptionDbKeyFactory = new val.db.sql.DbKey.LongKeyFactory<Subscription>("id") {
        @Override
        public DbKey newKey(Subscription subscription) {
            return subscription.dbKey;
        }
    };

    private final VersionedEntityTable<Subscription> subscriptionTable;

    public SqlSubscriptionStore(DerivedTableManager derivedTableManager) {
        subscriptionTable = new VersionedEntitySqlTable<Subscription>("subscription", Tables.SUBSCRIPTION, subscriptionDbKeyFactory, derivedTableManager) {
            @Override
            protected Subscription load(DSLContext ctx, ResultSet rs) throws SQLException {
                return new SqlSubscription(rs);
            }

            @Override
            protected void save(DSLContext ctx, Subscription subscription) {
                saveSubscription(ctx, subscription);
            }

            @Override
            protected List<SortField> defaultSort() {
                List<SortField> sort = new ArrayList<>();
                sort.add(tableClass.field("time_next", Integer.class).asc());
                sort.add(tableClass.field("id", Long.class).asc());
                return sort;
            }
        };
    }

    private static Condition getByParticipantClause(final long id) {
        return SUBSCRIPTION.SENDER_ID.eq(id).or(SUBSCRIPTION.RECIPIENT_ID.eq(id));
    }

    private static Condition getUpdateOnBlockClause(final int timestamp) {
        return SUBSCRIPTION.TIME_NEXT.le(timestamp);
    }

    @Override
    public DbKey.LongKeyFactory<Subscription> getSubscriptionDbKeyFactory() {
        return subscriptionDbKeyFactory;
    }

    @Override
    public VersionedEntityTable<Subscription> getSubscriptionTable() {
        return subscriptionTable;
    }

    @Override
    public DbIterator<Subscription> getSubscriptionsByParticipant(Long accountId) {
        return subscriptionTable.getManyBy(getByParticipantClause(accountId), 0, -1);
    }

    @Override
    public DbIterator<Subscription> getIdSubscriptions(Long accountId) {
        return subscriptionTable.getManyBy(SUBSCRIPTION.SENDER_ID.eq(accountId), 0, -1);
    }

    @Override
    public DbIterator<Subscription> getSubscriptionsToId(Long accountId) {
        return subscriptionTable.getManyBy(SUBSCRIPTION.RECIPIENT_ID.eq(accountId), 0, -1);
    }

    @Override
    public DbIterator<Subscription> getUpdateSubscriptions(int timestamp) {
        return subscriptionTable.getManyBy(getUpdateOnBlockClause(timestamp), 0, -1);
    }

    private void saveSubscription(DSLContext ctx, Subscription subscription) {
        SubscriptionRecord subscriptionRecord = ctx.newRecord(SUBSCRIPTION);
        subscriptionRecord.setId(subscription.id);
        subscriptionRecord.setSenderId(subscription.senderId);
        subscriptionRecord.setRecipientId(subscription.recipientId);
        subscriptionRecord.setAmount(subscription.amountNQT);
        subscriptionRecord.setFrequency(subscription.frequency);
        subscriptionRecord.setTimeNext(subscription.getTimeNext());
        subscriptionRecord.setHeight(ValNetwork.getBlockchain().getHeight());
        subscriptionRecord.setLatest(true);
        DbUtils.mergeInto(
                ctx, subscriptionRecord, SUBSCRIPTION,
                (
                        new Field[]{
                                subscriptionRecord.field("id"),
                                subscriptionRecord.field("sender_id"),
                                subscriptionRecord.field("recipient_id"),
                                subscriptionRecord.field("amount"),
                                subscriptionRecord.field("frequency"),
                                subscriptionRecord.field("time_next"),
                                subscriptionRecord.field("height"),
                                subscriptionRecord.field("latest")
                        }
                )
        );
    }

    private class SqlSubscription extends Subscription {
        SqlSubscription(ResultSet rs) throws SQLException {
            super(
                    rs.getLong("sender_id"),
                    rs.getLong("recipient_id"),
                    rs.getLong("id"),
                    rs.getLong("amount"),
                    rs.getInt("frequency"),
                    rs.getInt("time_next"),
                    subscriptionDbKeyFactory.newKey(rs.getLong("id"))

            );
        }


    }
}
