package val.services;

import val.Transaction;
import val.ValException;

public interface TransactionService {

    boolean verifyPublicKey(Transaction transaction);

    void validate(Transaction transaction) throws ValException.ValidationException;

    boolean applyUnconfirmed(Transaction transaction);

    void apply(Transaction transaction);

    void undoUnconfirmed(Transaction transaction);
}
