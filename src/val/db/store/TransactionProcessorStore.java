package val.db.store;

import val.Transaction;
import val.db.DbKey;
import val.db.sql.EntitySqlTable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TransactionProcessorStore {
    // WATCH: BUSINESS-LOGIC
    void processLater(Collection<Transaction> transactions);

    DbKey.LongKeyFactory<Transaction> getUnconfirmedTransactionDbKeyFactory();

    Set<Transaction> getLostTransactions();

    Map<Long, Integer> getLostTransactionHeights();

    EntitySqlTable<Transaction> getUnconfirmedTransactionTable();

    int deleteTransaction(Transaction transaction);

    boolean hasTransaction(long transactionId);
}
