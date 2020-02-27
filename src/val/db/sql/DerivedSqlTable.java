package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.db.DerivedTable;
import val.db.store.DerivedTableManager;

public abstract class DerivedSqlTable implements DerivedTable {
    private static final Logger logger = LoggerFactory.getLogger(DerivedSqlTable.class);
    final String table;
    final TableImpl<?> tableClass;

    DerivedSqlTable(String table, TableImpl<?> tableClass, DerivedTableManager derivedTableManager) {
        this.table = table;
        this.tableClass = tableClass;
        logger.trace("Creating derived table for " + table);
        System.out.printf("register derived table:%s\n", table);
        derivedTableManager.registerDerivedTable(this);
    }

    @Override
    public void rollback(int height) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DSLContext ctx = Db.getDSLContext();
        ctx.delete(tableClass).where(tableClass.field("height", Integer.class).gt(height)).execute();
    }

    @Override
    public void truncate() {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DSLContext ctx = Db.getDSLContext();
        ctx.delete(tableClass).execute();
    }

    @Override
    public void trim(int height) {
        //nothing to trim
    }

    @Override
    public void finish() {

    }

}
