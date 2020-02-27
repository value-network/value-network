package val.db.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class DbUtils {

    private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);

    private DbUtils() {
    } // never

    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static void applyPages(SelectQuery query, int page, int limit) {
        int start = (page - 1) * limit;
        if (limit > 0) {
            query.addLimit(limit);
        }
        if (start >= 0) {
            query.addOffset(start);
        }
    }


    public static void applyLimits(SelectQuery query, int from, int to) {
        int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
        if (limit > 0 && from > 0) {
            query.addLimit(from, limit);
        } else if (limit > 0) {
            query.addLimit(limit);
        } else if (from > 0) {
            query.addOffset(from);
        }
    }

    public static void mergeInto(DSLContext ctx, Record record, TableImpl table, Field[] keyFields) {
        // this is a hack .. we ignore always the first column on mergeInto commands to not fall over the db_id key
        ctx.mergeInto(table, Arrays.copyOfRange(record.fields(), 1, record.fields().length))
                .key(keyFields).values(Arrays.copyOfRange(record.valuesRow().fields(), 1, record.valuesRow().fields().length))
                .execute();
    }
}
