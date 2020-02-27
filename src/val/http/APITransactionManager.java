package val.http;

import com.google.gson.JsonElement;
import val.Account;
import val.Attachment;
import val.ValException;

import javax.servlet.http.HttpServletRequest;

public interface APITransactionManager {

    JsonElement createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT, Attachment attachment, long minimumFeeNQT) throws ValException;

}
