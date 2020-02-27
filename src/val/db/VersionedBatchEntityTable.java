package val.db;

import org.ehcache.Cache;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.SortField;

import java.util.ArrayList;
import java.util.List;

public interface VersionedBatchEntityTable<T> extends DerivedTable, EntityTable<T> {
    boolean delete(T t);

    @Override
    T get(DbKey dbKey);

    @Override
    void insert(T t);

    @Override
    void finish();

    @Override
    T get(DbKey dbKey, int height);

    @Override
    T getBy(Condition condition);

    @Override
    T getBy(Condition condition, int height);

    @Override
    DbIterator<T> getManyBy(Condition condition, int from, int to, List<SortField> sort);

    @Override
    DbIterator<T> getManyBy(Condition condition, int height, int from, int to);

    @Override
    DbIterator<T> getManyBy(Condition condition, int height, int from, int to, List<SortField> sort);

    @Override
    DbIterator<T> getManyBy(DSLContext ctx, SelectQuery query, boolean cache);

    @Override
    DbIterator<T> getAll(int from, int to);

    @Override
    DbIterator<T> getAll(int from, int to, List<SortField> sort);

    @Override
    DbIterator<T> getAll(int height, int from, int to);

    @Override
    DbIterator<T> getAll(int height, int from, int to, List<SortField> sort);

    @Override
    int getCount();

    @Override
    int getRowCount();

    @Override
    void rollback(int height);

    @Override
    void truncate();

    Cache getCache();

    void flushCache();

    void fillCache(ArrayList<Long> ids);
}
