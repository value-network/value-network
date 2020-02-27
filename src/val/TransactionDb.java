package val;

import org.jooq.DSLContext;
import val.schema.tables.records.TransactionRecord;

import java.sql.ResultSet;
import java.util.List;

public interface TransactionDb {
    List<Transaction> findTransactionsByType(byte type, byte subType);

    Transaction findTransaction(long transactionId);

    Transaction findTransactionByFullHash(String fullHash); // TODO add byte[] method

    boolean hasTransaction(long transactionId);

    boolean hasTransactionByFullHash(String fullHash); // TODO add byte[] method

    Transaction loadTransaction(TransactionRecord transactionRecord) throws ValException.ValidationException;

    Transaction loadTransaction(DSLContext ctx, ResultSet rs) throws ValException.ValidationException;

    List<Transaction> findBlockTransactions(long blockId);

    void saveTransactions(List<Transaction> transactions);

    List<Transaction> findTransactionByLikeFullHash(String fullHash);

    List<Long> getTransactionByLikeId(String transactionId);

}
