package val.db;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.SortField;

import java.util.List;

public interface EntityTable<T> extends DerivedTable {
    void checkAvailable(int height);

    T get(DbKey dbKey);

    T get(DbKey dbKey, int height);

    T getBy(Condition condition);

    T getBy(Condition condition, int height);

    DbIterator<T> getManyBy(Condition condition, int from, int to);

    DbIterator<T> getManyBy(Condition condition, int from, int to, List<SortField> sort);

    DbIterator<T> getManyBy(Condition condition, int height, int from, int to);

    DbIterator<T> getManyBy(Condition condition, int height, int from, int to, List<SortField> sort);

    DbIterator<T> getManyBy(DSLContext ctx, SelectQuery query, boolean cache);

    DbIterator<T> getAll(int from, int to);

    DbIterator<T> getAll(int from, int to, List<SortField> sort);

    DbIterator<T> getAll(int height, int from, int to);

    DbIterator<T> getAll(int height, int from, int to, List<SortField> sort);

    int getCount();

    int getRowCount();

    void insert(T t);

    @Override
    void rollback(int height);

    @Override
    void truncate();
}
