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
import val.schema.tables.records.RewardRecipAssignRecord;

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
public class RewardRecipAssign extends TableImpl<RewardRecipAssignRecord> {

    /**
     * The reference instance of <code>DB.reward_recip_assign</code>
     */
    public static final RewardRecipAssign REWARD_RECIP_ASSIGN = new RewardRecipAssign();
    private static final long serialVersionUID = -1000596737;
    /**
     * The column <code>DB.reward_recip_assign.db_id</code>.
     */
    public final TableField<RewardRecipAssignRecord, Long> DB_ID = createField("db_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).identity(true), this, "");
    /**
     * The column <code>DB.reward_recip_assign.account_id</code>.
     */
    public final TableField<RewardRecipAssignRecord, Long> ACCOUNT_ID = createField("account_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.reward_recip_assign.prev_recip_id</code>.
     */
    public final TableField<RewardRecipAssignRecord, Long> PREV_RECIP_ID = createField("prev_recip_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.reward_recip_assign.recip_id</code>.
     */
    public final TableField<RewardRecipAssignRecord, Long> RECIP_ID = createField("recip_id", org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "");
    /**
     * The column <code>DB.reward_recip_assign.from_height</code>.
     */
    public final TableField<RewardRecipAssignRecord, Integer> FROM_HEIGHT = createField("from_height", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");
    /**
     * The column <code>DB.reward_recip_assign.height</code>.
     */
    public final TableField<RewardRecipAssignRecord, Integer> HEIGHT = createField("height", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");
    /**
     * The column <code>DB.reward_recip_assign.latest</code>.
     */
    public final TableField<RewardRecipAssignRecord, Boolean> LATEST = createField("latest", org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.field("1", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * Create a <code>DB.reward_recip_assign</code> table reference
     */
    public RewardRecipAssign() {
        this(DSL.name("reward_recip_assign"), null);
    }

    /**
     * Create an aliased <code>DB.reward_recip_assign</code> table reference
     */
    public RewardRecipAssign(String alias) {
        this(DSL.name(alias), REWARD_RECIP_ASSIGN);
    }

    /**
     * Create an aliased <code>DB.reward_recip_assign</code> table reference
     */
    public RewardRecipAssign(Name alias) {
        this(alias, REWARD_RECIP_ASSIGN);
    }

    private RewardRecipAssign(Name alias, Table<RewardRecipAssignRecord> aliased) {
        this(alias, aliased, null);
    }

    private RewardRecipAssign(Name alias, Table<RewardRecipAssignRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RewardRecipAssignRecord> getRecordType() {
        return RewardRecipAssignRecord.class;
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
        return Arrays.<Index>asList(Indexes.REWARD_RECIP_ASSIGN_PRIMARY, Indexes.REWARD_RECIP_ASSIGN_REWARD_RECIP_ASSIGN_ACCOUNT_ID_HEIGHT_IDX, Indexes.REWARD_RECIP_ASSIGN_REWARD_RECIP_ASSIGN_RECIP_ID_HEIGHT_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<RewardRecipAssignRecord, Long> getIdentity() {
        return Keys.IDENTITY_REWARD_RECIP_ASSIGN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<RewardRecipAssignRecord> getPrimaryKey() {
        return Keys.KEY_REWARD_RECIP_ASSIGN_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<RewardRecipAssignRecord>> getKeys() {
        return Arrays.<UniqueKey<RewardRecipAssignRecord>>asList(Keys.KEY_REWARD_RECIP_ASSIGN_PRIMARY, Keys.KEY_REWARD_RECIP_ASSIGN_REWARD_RECIP_ASSIGN_ACCOUNT_ID_HEIGHT_IDX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RewardRecipAssign as(String alias) {
        return new RewardRecipAssign(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RewardRecipAssign as(Name alias) {
        return new RewardRecipAssign(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public RewardRecipAssign rename(String name) {
        return new RewardRecipAssign(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RewardRecipAssign rename(Name name) {
        return new RewardRecipAssign(name, null);
    }
}
