package val.transactionduplicates;

import val.Transaction;

public class TransactionDuplicationResult {

    final boolean duplicate;

    final Transaction transaction;

    public TransactionDuplicationResult(boolean duplicate, Transaction transaction) {
        this.duplicate = duplicate;
        this.transaction = transaction;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
