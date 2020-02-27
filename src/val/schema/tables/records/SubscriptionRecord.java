/*
 * This file is generated by jOOQ.
 */
package val.schema.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record9;
import org.jooq.Row9;
import org.jooq.impl.UpdatableRecordImpl;
import val.schema.tables.Subscription;

import javax.annotation.Generated;


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
public class SubscriptionRecord extends UpdatableRecordImpl<SubscriptionRecord> implements Record9<Long, Long, Long, Long, Long, Integer, Integer, Integer, Boolean> {

    private static final long serialVersionUID = 1905603366;

    /**
     * Create a detached SubscriptionRecord
     */
    public SubscriptionRecord() {
        super(Subscription.SUBSCRIPTION);
    }

    /**
     * Create a detached, initialised SubscriptionRecord
     */
    public SubscriptionRecord(Long dbId, Long id, Long senderId, Long recipientId, Long amount, Integer frequency, Integer timeNext, Integer height, Boolean latest) {
        super(Subscription.SUBSCRIPTION);

        set(0, dbId);
        set(1, id);
        set(2, senderId);
        set(3, recipientId);
        set(4, amount);
        set(5, frequency);
        set(6, timeNext);
        set(7, height);
        set(8, latest);
    }

    /**
     * Getter for <code>DB.subscription.db_id</code>.
     */
    public Long getDbId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>DB.subscription.db_id</code>.
     */
    public void setDbId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>DB.subscription.id</code>.
     */
    public Long getId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>DB.subscription.id</code>.
     */
    public void setId(Long value) {
        set(1, value);
    }

    /**
     * Getter for <code>DB.subscription.sender_id</code>.
     */
    public Long getSenderId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>DB.subscription.sender_id</code>.
     */
    public void setSenderId(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>DB.subscription.recipient_id</code>.
     */
    public Long getRecipientId() {
        return (Long) get(3);
    }

    /**
     * Setter for <code>DB.subscription.recipient_id</code>.
     */
    public void setRecipientId(Long value) {
        set(3, value);
    }

    /**
     * Getter for <code>DB.subscription.amount</code>.
     */
    public Long getAmount() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>DB.subscription.amount</code>.
     */
    public void setAmount(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>DB.subscription.frequency</code>.
     */
    public Integer getFrequency() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>DB.subscription.frequency</code>.
     */
    public void setFrequency(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>DB.subscription.time_next</code>.
     */
    public Integer getTimeNext() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>DB.subscription.time_next</code>.
     */
    public void setTimeNext(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>DB.subscription.height</code>.
     */
    public Integer getHeight() {
        return (Integer) get(7);
    }

    /**
     * Setter for <code>DB.subscription.height</code>.
     */
    public void setHeight(Integer value) {
        set(7, value);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * Getter for <code>DB.subscription.latest</code>.
     */
    public Boolean getLatest() {
        return (Boolean) get(8);
    }

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    /**
     * Setter for <code>DB.subscription.latest</code>.
     */
    public void setLatest(Boolean value) {
        set(8, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, Long, Long, Long, Integer, Integer, Integer, Boolean> fieldsRow() {
        return (Row9) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row9<Long, Long, Long, Long, Long, Integer, Integer, Integer, Boolean> valuesRow() {
        return (Row9) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return Subscription.SUBSCRIPTION.DB_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field2() {
        return Subscription.SUBSCRIPTION.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field3() {
        return Subscription.SUBSCRIPTION.SENDER_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field4() {
        return Subscription.SUBSCRIPTION.RECIPIENT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field5() {
        return Subscription.SUBSCRIPTION.AMOUNT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field6() {
        return Subscription.SUBSCRIPTION.FREQUENCY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field7() {
        return Subscription.SUBSCRIPTION.TIME_NEXT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field8() {
        return Subscription.SUBSCRIPTION.HEIGHT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Boolean> field9() {
        return Subscription.SUBSCRIPTION.LATEST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component2() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component3() {
        return getSenderId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component4() {
        return getRecipientId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long component5() {
        return getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component6() {
        return getFrequency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component7() {
        return getTimeNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer component8() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean component9() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getDbId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value2() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value3() {
        return getSenderId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value4() {
        return getRecipientId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value5() {
        return getAmount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value6() {
        return getFrequency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value7() {
        return getTimeNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value8() {
        return getHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean value9() {
        return getLatest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value1(Long value) {
        setDbId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value2(Long value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value3(Long value) {
        setSenderId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value4(Long value) {
        setRecipientId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value5(Long value) {
        setAmount(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value6(Integer value) {
        setFrequency(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value7(Integer value) {
        setTimeNext(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value8(Integer value) {
        setHeight(value);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord value9(Boolean value) {
        setLatest(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionRecord values(Long value1, Long value2, Long value3, Long value4, Long value5, Integer value6, Integer value7, Integer value8, Boolean value9) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        return this;
    }
}
