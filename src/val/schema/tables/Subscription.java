/*
 * This file is generated by jOOQ.
 */
package val.schema.tables;


import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import val.schema.Db;
import val.schema.Indexes;
import val.schema.Keys;
import val.schema.tables.records.SubscriptionRecord;

import javax.annotation.Generated;
import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@Generated(
        value = {
                "http://www.jooq.org",
                "jOOQ version:3.10.5"
        },
        comments = "This class is generated by jOOQ"
)
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Subscription extends TableImpl<SubscriptionRecord> {

    /**
     * The reference instance of <code>DB.subscription</code>
     */
    public static final Subscription SUBSCRIPTION = new Subscription();
    private static final long serialVersionUID = 113231284;
    /**
     * The column <code>DB.subscription.db_id</code>.
     */
    public final TableField<SubscriptionRecord, Long> DB_ID = createField("db_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");
    /**
     * The column <code>DB.subscription.id</code>.
     */
    public final TableField<SubscriptionRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.sender_id</code>.
     */
    public final TableField<SubscriptionRecord, Long> SENDER_ID = createField("sender_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.recipient_id</code>.
     */
    public final TableField<SubscriptionRecord, Long> RECIPIENT_ID = createField("recipient_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.amount</code>.
     */
    public final TableField<SubscriptionRecord, Long> AMOUNT = createField("amount", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.frequency</code>.
     */
    public final TableField<SubscriptionRecord, Integer> FREQUENCY = createField("frequency", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.time_next</code>.
     */
    public final TableField<SubscriptionRecord, Integer> TIME_NEXT = createField("time_next", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.height</code>.
     */
    public final TableField<SubscriptionRecord, Integer> HEIGHT = createField("height", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");
    /**
     * The column <code>DB.subscription.latest</code>.
     */
    public final TableField<SubscriptionRecord, Boolean> LATEST = createField("latest", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("1", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * Create a <code>DB.subscription</code> table reference
     */
    public Subscription() {
        this(DSL.name("subscription"), null);
    }

    /**
     * Create an aliased <code>DB.subscription</code> table reference
     */
    public Subscription(String alias) {
        this(DSL.name(alias), SUBSCRIPTION);
    }

    /**
     * Create an aliased <code>DB.subscription</code> table reference
     */
    public Subscription(Name alias) {
        this(alias, SUBSCRIPTION);
    }

    private Subscription(Name alias, Table<SubscriptionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Subscription(Name alias, Table<SubscriptionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * The class holding records for this type
     */
    @Override
    public Class<SubscriptionRecord> getRecordType() {
        return SubscriptionRecord.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Db.DB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.SUBSCRIPTION_PRIMARY, Indexes.SUBSCRIPTION_SUBSCRIPTION_ID_HEIGHT_IDX, Indexes.SUBSCRIPTION_SUBSCRIPTION_RECIPIENT_ID_HEIGHT_IDX, Indexes.SUBSCRIPTION_SUBSCRIPTION_SENDER_ID_HEIGHT_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<SubscriptionRecord, Long> getIdentity() {
        return Keys.IDENTITY_SUBSCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<SubscriptionRecord> getPrimaryKey() {
        return Keys.KEY_SUBSCRIPTION_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<SubscriptionRecord>> getKeys() {
        return Arrays.<UniqueKey<SubscriptionRecord>>asList(Keys.KEY_SUBSCRIPTION_PRIMARY, Keys.KEY_SUBSCRIPTION_SUBSCRIPTION_ID_HEIGHT_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription as(String alias) {
        return new Subscription(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription as(Name alias) {
        return new Subscription(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Subscription rename(String name) {
        return new Subscription(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Subscription rename(Name name) {
        return new Subscription(name, null);
    }
}
