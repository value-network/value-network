package val.db.sql;

import org.jooq.impl.TableImpl;
import val.db.VersionedValuesTable;
import val.db.store.DerivedTableManager;

public abstract class VersionedValuesSqlTable<T, V> extends ValuesSqlTable<T, V> implements VersionedValuesTable<T, V> {
    VersionedValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, DerivedTableManager derivedTableManager) {
        super(table, tableClass, dbKeyFactory, true, derivedTableManager);
    }

    @Override
    public final void rollback(int height) {
        VersionedEntitySqlTable.rollback(table, tableClass, height, dbKeyFactory);
    }

    @Override
    public final void trim(int height) {
        VersionedEntitySqlTable.trim(table, tableClass, height, dbKeyFactory);
    }
}
