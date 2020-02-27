package val;

import com.google.gson.JsonObject;
import val.peer.Peer;
import val.util.Observable;

import java.util.List;

public interface TransactionProcessor extends Observable<List<? extends Transaction>, TransactionProcessor.Event> {

    List<Transaction> getAllUnconfirmedTransactions();

    int getAmountUnconfirmedTransactions();

    List<Transaction> getAllUnconfirmedTransactionsFor(Peer peer);

    void markFingerPrintsOf(Peer peer, List<Transaction> transactions);

    Transaction getUnconfirmedTransaction(long transactionId);

    void clearUnconfirmedTransactions();

    Integer broadcast(Transaction transaction) throws ValException.ValidationException;

    void processPeerTransactions(JsonObject request, Peer peer) throws ValException.ValidationException;

    Transaction parseTransaction(byte[] bytes) throws ValException.ValidationException;

    Transaction parseTransaction(JsonObject json) throws ValException.ValidationException;

    Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment);

    Transaction.Builder newTransactionBuilderWithTimestamp(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment, int timestamp);

    enum Event {
        REMOVED_UNCONFIRMED_TRANSACTIONS,
        ADDED_UNCONFIRMED_TRANSACTIONS,
        ADDED_CONFIRMED_TRANSACTIONS,
        ADDED_DOUBLESPENDING_TRANSACTIONS
    }

}
