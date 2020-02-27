package val.transactionduplicates;

import val.TransactionType;

public class TransactionDuplicationKey {

    public static final TransactionDuplicationKey IS_ALWAYS_DUPLICATE = new TransactionDuplicationKey(null, "always");
    public static final TransactionDuplicationKey IS_NEVER_DUPLICATE = new TransactionDuplicationKey(null, "never");
    final TransactionType transactionType;
    final String key;

    public TransactionDuplicationKey(TransactionType transactionType, String key) {
        this.transactionType = transactionType;
        this.key = key;
    }
}
