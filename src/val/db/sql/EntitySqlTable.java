package val.db.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import val.ValNetwork;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.EntityTable;
import val.db.store.DerivedTableManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class EntitySqlTable<T> extends DerivedSqlTable implements EntityTable<T> {
    final val.db.sql.DbKey.Factory<T> dbKeyFactory;
    private final boolean multiversion;
    private final List<SortField> defaultSort;

    EntitySqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, DerivedTableManager derivedTableManager) {
        this(table, tableClass, dbKeyFactory, false, derivedTableManager);
    }

    EntitySqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, boolean multiversion, DerivedTableManager derivedTableManager) {
        super(table, tableClass, derivedTableManager);
        this.dbKeyFactory = (val.db.sql.DbKey.Factory<T>) dbKeyFactory;
        this.multiversion = multiversion;
        this.defaultSort = new ArrayList<>();
        if (multiversion) {
            for (String column : this.dbKeyFactory.getPKColumns()) {
                defaultSort.add(tableClass.field(column, Long.class).asc());
            }
        }
        defaultSort.add(tableClass.field("height", Integer.class).desc());
    }

    protected abstract T load(DSLContext ctx, ResultSet rs) throws SQLException;

    void save(DSLContext ctx, T t) {
    }

    List<SortField> defaultSort() {
        return defaultSort;
    }

    @Override
    public final void checkAvailable(int height) {
        if (multiversion && height < ValNetwork.getBlockchainProcessor().getMinRollbackHeight()) {
            throw new IllegalArgumentException("Historical data as of height " + height + " not available, set vlm.trimDerivedTables=false and re-scan");
        }
    }

    @Override
    public T get(DbKey nxtKey) {
        val.db.sql.DbKey dbKey = (val.db.sql.DbKey) nxtKey;
        if (Db.isInTransaction()) {
            T t = (T) Db.getCache(table).get(dbKey);
            if (t != null) {
                return t;
            }
        }
        try (DSLContext ctx = Db.getDSLContext()) {
            SelectQuery query = ctx.selectQuery();
            query.addFrom(tableClass);
            query.addConditions(dbKey.getPKConditions(tableClass));
            if (multiversion) {
                query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
            }
            query.addLimit(1);

            return get(ctx, query, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T get(DbKey nxtKey, int height) {
        val.db.sql.DbKey dbKey = (val.db.sql.DbKey) nxtKey;
        checkAvailable(height);

        try (DSLContext ctx = Db.getDSLContext()) {
            SelectQuery query = ctx.selectQuery();
            query.addFrom(tableClass);
            query.addConditions(dbKey.getPKConditions(tableClass));
            query.addConditions(tableClass.field("height", Integer.class).le(height));
            if (multiversion) {
                Table innerTable = tableClass.as("b");
                SelectQuery innerQuery = ctx.selectQuery();
                innerQuery.addConditions(innerTable.field("height", Integer.class).gt(height));
                innerQuery.addConditions(dbKey.getPKConditions(innerTable));
                // ToDo: verify:
                // (latest = TRUE OR EXISTS ( SELECT 1 FROM " + table + dbKeyFactory.getPKClause() + " AND height > ?))"
                query.addConditions(
                        tableClass.field("latest", Boolean.class).isTrue().or(
                                DSL.field(DSL.exists(innerQuery))
                        )
                );
            }
            query.addOrderBy(tableClass.field("height").desc());
            query.addLimit(1);

            return get(ctx, query, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T getBy(Condition condition) {
        try (DSLContext ctx = Db.getDSLContext()) {
            SelectQuery query = ctx.selectQuery();
            query.addFrom(tableClass);
            query.addConditions(condition);
            if (multiversion) {
                query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
            }
            query.addLimit(1);

            return get(ctx, query, true);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public T getBy(Condition condition, int height) {
        checkAvailable(height);

        try (DSLContext ctx = Db.getDSLContext()) {
            SelectQuery query = ctx.selectQuery();
            query.addFrom(tableClass);
            query.addConditions(condition);
            query.addConditions(tableClass.field("height", Integer.class).le(height));
            if (multiversion) {
                Table innerTable = tableClass.as("b");
                SelectQuery innerQuery = ctx.selectQuery();
                innerQuery.addConditions(innerTable.field("height", Integer.class).gt(height));
                dbKeyFactory.applySelfJoin(innerQuery, innerTable, tableClass);
                query.addConditions(
                        tableClass.field("latest", Boolean.class).isTrue().or(
                                DSL.field(DSL.exists(innerQuery))
                        )
                );
            }
            query.addOrderBy(tableClass.field("height").desc());
            query.addLimit(1);

            return get(ctx, query, false);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private T get(DSLContext ctx, SelectQuery query, boolean cache) throws SQLException {
        final boolean doCache = cache && Db.isInTransaction();
        try (ResultSet rs = query.fetchResultSet()) {
            if (!rs.next()) {
                return null;
            }
            T t = null;
            val.db.sql.DbKey dbKey = null;
            if (doCache) {
                dbKey = (val.db.sql.DbKey) dbKeyFactory.newKey(rs);
                t = (T) Db.getCache(table).get(dbKey);
            }
            if (t == null) {
                t = load(ctx, rs);
                if (doCache) {
                    Db.getCache(table).put(dbKey, t);
                }
            }
            if (rs.next()) {
                throw new RuntimeException("Multiple records found");
            }
            return t;
        }
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int from, int to) {
        return getManyBy(condition, from, to, defaultSort());
    }

    @Override
    public val.db.DbIterator<T> getManyBy(Condition condition, int from, int to, List<SortField> sort) {
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectQuery();
        query.addFrom(tableClass);
        query.addConditions(condition);
        query.addOrderBy(sort);
        if (multiversion) {
            query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
        }
        DbUtils.applyLimits(query, from, to);
        return getManyBy(ctx, query, true);
    }

    @Override
    public val.db.DbIterator<T> getManyBy(Condition condition, int height, int from, int to) {
        return getManyBy(condition, height, from, to, defaultSort());
    }

    @Override
    public DbIterator<T> getManyBy(Condition condition, int height, int from, int to, List<SortField> sort) {
        checkAvailable(height);
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectQuery();
        query.addFrom(tableClass);
        query.addConditions(condition);
        query.addConditions(tableClass.field("height", Integer.class).le(height));
        if (multiversion) {
            Table innerTableB = tableClass.as("b");
            SelectQuery innerQueryB = ctx.selectQuery();
            innerQueryB.addConditions(innerTableB.field("height", Integer.class).gt(height));
            dbKeyFactory.applySelfJoin(innerQueryB, innerTableB, tableClass);

            Table innerTableC = tableClass.as("c");
            SelectQuery innerQueryC = ctx.selectQuery();
            innerQueryC.addConditions(
                    innerTableC.field("height", Integer.class).le(height).and(
                            innerTableC.field("height").gt(tableClass.field("height"))
                    )
            );
            dbKeyFactory.applySelfJoin(innerQueryC, innerTableC, tableClass);

            query.addConditions(
                    tableClass.field("latest", Boolean.class).isTrue().or(
                            DSL.field(
                                    DSL.exists(innerQueryB).and(DSL.notExists(innerQueryC))
                            )
                    )
            );
        }
        query.addOrderBy(sort);

        DbUtils.applyLimits(query, from, to);
        return getManyBy(ctx, query, true);
    }

    public val.db.DbIterator<T> getManyBy(DSLContext ctx, SelectQuery query, boolean cache) {
        final boolean doCache = cache && Db.isInTransaction();
        return new val.db.sql.DbIterator(ctx, query.fetchResultSet(), (ctx1, rs) -> {
            T t = null;
            val.db.sql.DbKey dbKey = null;
            if (doCache) {
                dbKey = (val.db.sql.DbKey) dbKeyFactory.newKey(rs);
                t = (T) Db.getCache(table).get(dbKey);
            }
            if (t == null) {
                t = load(ctx1, rs);
                if (doCache) {
                    Db.getCache(table).put(dbKey, t);
                }
            }
            return t;
        });
    }

    @Override
    public DbIterator<T> getAll(int from, int to) {
        return getAll(from, to, defaultSort());
    }

    @Override
    public DbIterator<T> getAll(int from, int to, List<SortField> sort) {
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectQuery();
        query.addFrom(tableClass);
        if (multiversion) {
            query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
        }
        query.addOrderBy(sort);
        DbUtils.applyLimits(query, from, to);
        return getManyBy(ctx, query, true);
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to) {
        return getAll(height, from, to, defaultSort());
    }

    @Override
    public DbIterator<T> getAll(int height, int from, int to, List<SortField> sort) {
        checkAvailable(height);
        DSLContext ctx = Db.getDSLContext();
        SelectQuery query = ctx.selectQuery();
        query.addFrom(tableClass);
        query.addConditions(tableClass.field("height", Integer.class).le(height));
        if (multiversion) {
            Table innerTableB = tableClass.as("b");
            SelectQuery innerQueryB = ctx.selectQuery();
            innerQueryB.addConditions(innerTableB.field("height", Integer.class).gt(height));
            dbKeyFactory.applySelfJoin(innerQueryB, innerTableB, tableClass);

            Table innerTableC = tableClass.as("c");
            SelectQuery innerQueryC = ctx.selectQuery();
            innerQueryC.addConditions(
                    innerTableC.field("height", Integer.class).le(height).and(
                            innerTableC.field("height").gt(tableClass.field("height"))
                    )
            );
            dbKeyFactory.applySelfJoin(innerQueryC, innerTableC, tableClass);

            query.addConditions(
                    tableClass.field("latest", Boolean.class).isTrue().or(
                            DSL.field(
                                    DSL.exists(innerQueryB).and(DSL.notExists(innerQueryC))
                            )
                    )
            );
        }
        query.addOrderBy(sort);
        query.addLimit(from, to);
        return getManyBy(ctx, query, true);
    }

    @Override
    public int getCount() {
        DSLContext ctx = Db.getDSLContext();
        TableImpl<?> t = tableClass;
        SelectJoinStep<?> r = ctx.selectCount().from(t);
        return (multiversion ? r.where(t.field("latest").isTrue()) : r).fetchOne(0, int.class);
    }

    @Override
    public int getRowCount() {
        DSLContext ctx = Db.getDSLContext();
        return ctx.selectCount().from(tableClass).fetchOne(0, int.class);
    }

    @Override
    public void insert(T t) {
        if (!Db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        val.db.sql.DbKey dbKey = (val.db.sql.DbKey) dbKeyFactory.newKey(t);
        T cachedT = (T) Db.getCache(table).get(dbKey);
        if (cachedT == null) {
            Db.getCache(table).put(dbKey, t);
        } else if (t != cachedT) { // not a bug
            throw new IllegalStateException("Different instance found in Db cache, perhaps trying to save an object "
                    + "that was read outside the current transaction");
        }
        try (DSLContext ctx = Db.getDSLContext()) {
            if (multiversion) {
                UpdateQuery query = ctx.updateQuery(tableClass);
                query.addValue(
                        tableClass.field("latest", Boolean.class),
                        false
                );
                query.addConditions(dbKey.getPKConditions(tableClass));
                query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
                query.execute();
            }
            save(ctx, t);
        }
    }

    @Override
    public void rollback(int height) {
        super.rollback(height);
        Db.getCache(table).clear();
    }

    @Override
    public void truncate() {
        super.truncate();
        Db.getCache(table).clear();
    }

}
