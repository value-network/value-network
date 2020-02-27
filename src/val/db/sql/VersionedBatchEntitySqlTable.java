package val.db.sql;

import org.ehcache.Cache;
import org.jooq.*;
import org.jooq.impl.TableImpl;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedBatchEntityTable;
import val.db.cache.DBCacheManagerImpl;
import val.db.store.DerivedTableManager;

import java.util.*;

public abstract class VersionedBatchEntitySqlTable<T> extends VersionedEntitySqlTable<T> implements VersionedBatchEntityTable<T> {

    private final DBCacheManagerImpl dbCacheManager;

    VersionedBatchEntitySqlTable(String table, TableImpl<?> tableClass, val.db.sql.DbKey.Factory<T> dbKeyFactory, DerivedTableManager derivedTableManager, DBCacheManagerImpl dbCacheManager) {
        super(table, tableClass, dbKeyFactory, derivedTableManager);
        this.dbCacheManager = dbCacheManager;
    }

    protected abstract void bulkInsert(DSLContext ctx, ArrayList<T> t);

    @Override
    public boolean delete(T t) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        val.db.sql.DbKey dbKey = (val.db.sql.DbKey) dbKeyFactory.newKey(t);
        getCache().remove(dbKey);
        Db.getBatch(table).remove(dbKey, null);
        Db.getCache(table).remove(dbKey);

        return true;
    }

    @Override
    public T get(DbKey dbKey) {
        if (getCache().containsKey(dbKey)) {
            return (T) getCache().get(dbKey);
        } else if (Db.isInTransaction()) {
            if (Db.getBatch(table).containsKey(dbKey)) {
                return (T) Db.getBatch(table).get(dbKey);
            }
        }
        T item = super.get(dbKey);
        if (item != null) {
            getCache().put(dbKey, item);
        }
        return item;
    }

    @Override
    public void insert(T t) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        val.db.sql.DbKey dbKey = (val.db.sql.DbKey) dbKeyFactory.newKey(t);
        Db.getBatch(table).put(dbKey, t);
        Db.getCache(table).put(dbKey, t);
        getCache().put(dbKey, t);
    }

    @Override
    public void finish() {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DSLContext ctx = Db.getDSLContext();
        Set keySet = Db.getBatch(table).keySet();
        if (!keySet.isEmpty()) {
            UpdateQuery updateQuery = ctx.updateQuery(tableClass);
            updateQuery.addValue(tableClass.field("latest", Boolean.class), false);
            Arrays.asList(dbKeyFactory.getPKColumns()).forEach(idColumn -> updateQuery.addConditions(tableClass.field(idColumn, Long.class).eq(0L)));
            updateQuery.addConditions(tableClass.field("latest", Boolean.class).isTrue());
            BatchBindStep updateBatch = ctx.batch(updateQuery);
            for (val.db.sql.DbKey dbKey : (Iterable<val.db.sql.DbKey>) keySet) {
                ArrayList<Object> bindArgs = new ArrayList<>();
                bindArgs.add(false);
                Arrays.stream(dbKey.getPKValues()).forEach(bindArgs::add);
                updateBatch = updateBatch.bind(bindArgs.toArray());
            }
            updateBatch.execute();
        }

        List<Map.Entry<val.db.sql.DbKey, Object>> entries = new ArrayList<>(Db.getBatch(table).entrySet());
        HashMap<val.db.sql.DbKey, T> itemOf = new HashMap<>();
        for (Map.Entry<val.db.sql.DbKey, Object> entry : entries) {
            if (entry.getValue() != null) {
                itemOf.put(entry.getKey(), (T) entry.getValue());
            }
        }
        if (itemOf.size() > 0) {
            bulkInsert(ctx, new ArrayList<>(itemOf.values()));
        }
        Db.getBatch(table).clear();
    }

    @Override
    public T get(DbKey dbKey, int height) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.get(dbKey, height);
    }

    @Override
    public T getBy(Condition condition) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getBy(condition);
    }

    @Override
    public T getBy(Condition condition, int height) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getBy(condition, height);
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int from, int to) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(condition, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int from, int to, List<SortField> sort) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(condition, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int height, int from, int to) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(condition, height, from, to);
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int height, int from, int to, List<SortField> sort) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(condition, height, from, to, sort);
    }

    @Override
    public DbIterator<T> getManyBy(DSLContext ctx, SelectQuery query, boolean cache) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getManyBy(ctx, query, cache);
    }

    @Override
    public DbIterator<T> getAll(int from, int to) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to);
    }

    @Override
    public DbIterator<T> getAll(int from, int to, List<SortField> sort) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(from, to, sort);
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(height, from, to);
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to, List<SortField> sort) {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getAll(height, from, to, sort);
    }

    @Override
    public int getCount() {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getCount();
    }

    @Override
    public int getRowCount() {
        if (Db.isInTransaction()) {
            throw new IllegalStateException("Cannot use in batch table transaction");
        }
        return super.getRowCount();
    }

    @Override
    public void rollback(int height) {
        super.rollback(height);
        Db.getBatch(table).clear();
    }

    @Override
    public void truncate() {
        super.truncate();
        Db.getBatch(table).clear();
    }

    @Override
    public Cache getCache() {
        return dbCacheManager.getCache(table);
    }

    @Override
    public void flushCache() {
        getCache().clear();
    }

    @Override
    public void fillCache(ArrayList<Long> ids) {
    }

}
