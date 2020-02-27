package val.http;

import com.google.gson.JsonElement;
import val.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static val.fluxcapacitor.FeatureToggle.PRE_DYMAXION;
import static val.http.common.Parameters.*;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[]{
            SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER, FEE_NQT_PARAMETER,
            DEADLINE_PARAMETER, REFERENCED_TRANSACTION_FULL_HASH_PARAMETER, BROADCAST_PARAMETER,
            MESSAGE_PARAMETER, MESSAGE_IS_TEXT_PARAMETER,
            MESSAGE_TO_ENCRYPT_PARAMETER, MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER, ENCRYPTED_MESSAGE_DATA_PARAMETER, ENCRYPTED_MESSAGE_NONCE_PARAMETER,
            MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER, MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER, ENCRYPT_TO_SELF_MESSAGE_DATA, ENCRYPT_TO_SELF_MESSAGE_NONCE,
            RECIPIENT_PUBLIC_KEY_PARAMETER};

    private final APITransactionManager apiTransactionManager;

    CreateTransaction(APITag[] apiTags, APITransactionManager apiTransactionManager, boolean replaceParameters, String... parameters) {
        super(apiTags, replaceParameters ? parameters : addCommonParameters(parameters));
        this.apiTransactionManager = apiTransactionManager;
    }

    CreateTransaction(APITag[] apiTags, APITransactionManager apiTransactionManager, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
        this.apiTransactionManager = apiTransactionManager;
    }

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    final JsonElement createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
            throws ValException {
        return createTransaction(req, senderAccount, null, 0, attachment);
    }

    final JsonElement createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT)
            throws ValException {
        return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
    }

    final JsonElement createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT, Attachment attachment) throws ValException {
        return apiTransactionManager.createTransaction(req, senderAccount, recipientId, amountNQT, attachment, minimumFeeNQT());
    }

    @Override
    final boolean requirePost() {
        return true;
    }

    private long minimumFeeNQT() {
        return ValNetwork.getFluxCapacitor().isActive(PRE_DYMAXION) ? Constants.FEE_QUANT : Constants.ONE_COIN;
    }

}
