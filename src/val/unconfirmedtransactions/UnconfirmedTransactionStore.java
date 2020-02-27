package val.unconfirmedtransactions;

import val.Transaction;
import val.ValException;
import val.peer.Peer;

import java.util.List;

public interface UnconfirmedTransactionStore {

    boolean put(Transaction transaction, Peer peer) throws ValException.ValidationException;

    Transaction get(Long transactionId);

    boolean exists(Long transactionId);

    List<Transaction> getAll();

    List<Transaction> getAllFor(Peer peer);

    void remove(Transaction transaction);

    void clear();

    /**
     * Review which transactions are still eligible to stay
     *
     * @return The list of removed transactions
     */
    void resetAccountBalances();

    void markFingerPrintsOf(Peer peer, List<Transaction> transactions);

    void removeForgedTransactions(List<Transaction> transactions);

    int getAmount();
}
