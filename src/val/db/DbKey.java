package val.db;

import java.sql.ResultSet;

public interface DbKey {

    long[] getPKValues();

    interface Factory<T> {
        DbKey newKey(T t);

        DbKey newKey(ResultSet rs);
    }

    interface LongKeyFactory<T> extends Factory<T> {
        @Override
        DbKey newKey(ResultSet rs);

        DbKey newKey(long id);

    }

    interface LinkKeyFactory<T> extends Factory<T> {
        DbKey newKey(long idA, long idB);
    }
}
