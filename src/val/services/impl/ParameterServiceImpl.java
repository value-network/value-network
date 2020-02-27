package val.services.impl;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.*;
import val.assetexchange.AssetExchange;
import val.crypto.Crypto;
import val.crypto.EncryptedData;
import val.http.JSONResponses;
import val.http.ParameterException;
import val.http.common.Parameters;
import val.http.common.ResultFields;
import val.services.*;
import val.util.Convert;
import val.util.JSON;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static val.http.JSONResponses.*;
import static val.http.common.Parameters.*;

public class ParameterServiceImpl implements ParameterService {

    private static final Logger logger = LoggerFactory.getLogger(ParameterServiceImpl.class);

    private final AccountService accountService;
    private final AliasService aliasService;
    private final AssetExchange assetExchange;
    private final DGSGoodsStoreService dgsGoodsStoreService;
    private final Blockchain blockchain;
    private final BlockchainProcessor blockchainProcessor;
    private final TransactionProcessor transactionProcessor;
    private final ATService atService;

    public ParameterServiceImpl(AccountService accountService, AliasService aliasService, AssetExchange assetExchange, DGSGoodsStoreService dgsGoodsStoreService, Blockchain blockchain,
                                BlockchainProcessor blockchainProcessor,
                                TransactionProcessor transactionProcessor, ATService atService) {
        this.accountService = accountService;
        this.aliasService = aliasService;
        this.assetExchange = assetExchange;
        this.dgsGoodsStoreService = dgsGoodsStoreService;
        this.blockchain = blockchain;
        this.blockchainProcessor = blockchainProcessor;
        this.transactionProcessor = transactionProcessor;
        this.atService = atService;
    }

    @Override
    public Account getAccount(HttpServletRequest req) throws ValException {
        String accountId = Convert.emptyToNull(req.getParameter(Parameters.ACCOUNT_PARAMETER));
        if (accountId == null) {
            throw new ParameterException(JSONResponses.MISSING_ACCOUNT);
        }
        try {
            Account account = accountService.getAccount(Convert.parseAccountId(accountId));
            if (account == null) {
                throw new ParameterException(JSONResponses.UNKNOWN_ACCOUNT);
            }
            return account;
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_ACCOUNT);
        }
    }

    @Override
    public List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
        String[] accountIDs = req.getParameterValues(Parameters.ACCOUNT_PARAMETER);
        if (accountIDs == null || accountIDs.length == 0) {
            throw new ParameterException(JSONResponses.MISSING_ACCOUNT);
        }
        List<Account> result = new ArrayList<>();
        for (String accountValue : accountIDs) {
            if (accountValue == null || accountValue.isEmpty()) {
                continue;
            }
            try {
                Account account = accountService.getAccount(Convert.parseAccountId(accountValue));
                if (account == null) {
                    throw new ParameterException(JSONResponses.UNKNOWN_ACCOUNT);
                }
                result.add(account);
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.INCORRECT_ACCOUNT);
            }
        }
        return result;
    }

    @Override
    public Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        Account account;
        String secretPhrase = Convert.emptyToNull(req.getParameter(Parameters.SECRET_PHRASE_PARAMETER));
        String publicKeyString = Convert.emptyToNull(req.getParameter(Parameters.PUBLIC_KEY_PARAMETER));
        if (secretPhrase != null) {
            account = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
        } else if (publicKeyString != null) {
            try {
                account = accountService.getAccount(Convert.parseHexString(publicKeyString));
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.INCORRECT_PUBLIC_KEY);
            }
        } else {
            throw new ParameterException(JSONResponses.MISSING_SECRET_PHRASE_OR_PUBLIC_KEY);
        }
        if (account == null) {
            throw new ParameterException(JSONResponses.UNKNOWN_ACCOUNT);
        }
        return account;
    }

    @Override
    public Alias getAlias(HttpServletRequest req) throws ParameterException {
        long aliasId;
        try {
            aliasId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(Parameters.ALIAS_PARAMETER)));
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_ALIAS);
        }
        String aliasName = Convert.emptyToNull(req.getParameter(Parameters.ALIAS_NAME_PARAMETER));
        Alias alias;
        if (aliasId != 0) {
            alias = aliasService.getAlias(aliasId);
        } else if (aliasName != null) {
            alias = aliasService.getAlias(aliasName);
        } else {
            throw new ParameterException(JSONResponses.MISSING_ALIAS_OR_ALIAS_NAME);
        }
        if (alias == null) {
            throw new ParameterException(JSONResponses.UNKNOWN_ALIAS);
        }
        return alias;
    }

    @Override
    public Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = Convert.emptyToNull(req.getParameter(Parameters.ASSET_PARAMETER));
        if (assetValue == null) {
            throw new ParameterException(JSONResponses.MISSING_ASSET);
        }
        Asset asset;
        try {
            long assetId = Convert.parseUnsignedLong(assetValue);
            asset = assetExchange.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_ASSET);
        }
        if (asset == null) {
            throw new ParameterException(JSONResponses.UNKNOWN_ASSET);
        }
        return asset;
    }

    @Override
    public DigitalGoodsStore.Goods getGoods(HttpServletRequest req) throws ParameterException {
        String goodsValue = Convert.emptyToNull(req.getParameter(Parameters.GOODS_PARAMETER));
        if (goodsValue == null) {
            throw new ParameterException(JSONResponses.MISSING_GOODS);
        }

        try {
            long goodsId = Convert.parseUnsignedLong(goodsValue);
            DigitalGoodsStore.Goods goods = dgsGoodsStoreService.getGoods(goodsId);
            if (goods == null) {
                throw new ParameterException(JSONResponses.UNKNOWN_GOODS);
            }
            return goods;
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_GOODS);
        }
    }

    @Override
    public DigitalGoodsStore.Purchase getPurchase(HttpServletRequest req) throws ParameterException {
        String purchaseIdString = Convert.emptyToNull(req.getParameter(Parameters.PURCHASE_PARAMETER));
        if (purchaseIdString == null) {
            throw new ParameterException(JSONResponses.MISSING_PURCHASE);
        }
        try {
            DigitalGoodsStore.Purchase purchase = dgsGoodsStoreService.getPurchase(Convert.parseUnsignedLong(purchaseIdString));
            if (purchase == null) {
                throw new ParameterException(JSONResponses.INCORRECT_PURCHASE);
            }
            return purchase;
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_PURCHASE);
        }
    }

    @Override
    public EncryptedData getEncryptedMessage(HttpServletRequest req, Account recipientAccount, byte[] publicKey) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter(Parameters.ENCRYPTED_MESSAGE_DATA_PARAMETER));
        String nonce = Convert.emptyToNull(req.getParameter(Parameters.ENCRYPTED_MESSAGE_NONCE_PARAMETER));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter(Parameters.MESSAGE_TO_ENCRYPT_PARAMETER));
        if (plainMessage == null) {
            return null;
        }

        String secretPhrase = getSecretPhrase(req);
        boolean isText = Parameters.isTrue(req.getParameter(Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            if (recipientAccount != null && recipientAccount.getPublicKey() != null) {
                return recipientAccount.encryptTo(plainMessageBytes, secretPhrase);
            } else if (publicKey != null) {
                return Account.encryptTo(plainMessageBytes, secretPhrase, publicKey);
            } else {
                throw new ParameterException(JSONResponses.INCORRECT_RECIPIENT);
            }
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_PLAIN_MESSAGE);
        }
    }

    @Override
    public EncryptedData getEncryptToSelfMessage(HttpServletRequest req) throws ParameterException {
        String data = Convert.emptyToNull(req.getParameter(Parameters.ENCRYPT_TO_SELF_MESSAGE_DATA));
        String nonce = Convert.emptyToNull(req.getParameter(Parameters.ENCRYPT_TO_SELF_MESSAGE_NONCE));
        if (data != null && nonce != null) {
            try {
                return new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.INCORRECT_ENCRYPTED_MESSAGE);
            }
        }
        String plainMessage = Convert.emptyToNull(req.getParameter(Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER));
        if (plainMessage == null) {
            return null;
        }
        String secretPhrase = getSecretPhrase(req);
        Account senderAccount = accountService.getAccount(Crypto.getPublicKey(secretPhrase));
        boolean isText = !Parameters.isFalse(req.getParameter(Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER));
        try {
            byte[] plainMessageBytes = isText ? Convert.toBytes(plainMessage) : Convert.parseHexString(plainMessage);
            return senderAccount.encryptTo(plainMessageBytes, secretPhrase);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_PLAIN_MESSAGE);
        }
    }

    @Override
    public String getSecretPhrase(HttpServletRequest req) throws ParameterException {
        String secretPhrase = Convert.emptyToNull(req.getParameter(Parameters.SECRET_PHRASE_PARAMETER));
        if (secretPhrase == null) {
            throw new ParameterException(JSONResponses.MISSING_SECRET_PHRASE);
        }
        return secretPhrase;
    }

    @Override
    public int getNumberOfConfirmations(HttpServletRequest req) throws ParameterException {
        String numberOfConfirmationsValue = Convert.emptyToNull(req.getParameter(Parameters.NUMBER_OF_CONFIRMATIONS_PARAMETER));
        if (numberOfConfirmationsValue != null) {
            try {
                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                if (numberOfConfirmations <= blockchain.getHeight()) {
                    return numberOfConfirmations;
                }
                throw new ParameterException(JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS);
            } catch (NumberFormatException e) {
                throw new ParameterException(JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS);
            }
        }
        return 0;
    }

    @Override
    public int getHeight(HttpServletRequest req) throws ParameterException {
        String heightValue = Convert.emptyToNull(req.getParameter(Parameters.HEIGHT_PARAMETER));
        if (heightValue != null) {
            try {
                int height = Integer.parseInt(heightValue);
                if (height < 0 || height > blockchain.getHeight()) {
                    throw new ParameterException(JSONResponses.INCORRECT_HEIGHT);
                }
                if (height < blockchainProcessor.getMinRollbackHeight()) {
                    throw new ParameterException(JSONResponses.HEIGHT_NOT_AVAILABLE);
                }
                return height;
            } catch (NumberFormatException e) {
                throw new ParameterException(JSONResponses.INCORRECT_HEIGHT);
            }
        }
        return -1;
    }

    @Override
    public boolean verifyPoolMinerSignature(HttpServletRequest req) throws ParameterException {
        String minerValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
        if (minerValue == null) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        String signature = Convert.emptyToNull(req.getParameter(SIGNATURE_PARAMETER));
        if (signature == null) {
            throw new ParameterException(MISSING_SIGNATURE);
        }
        String publicKey = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
        if (publicKey == null) {
            throw new ParameterException(MISSING_PUBLIC_KEY);
        }
        System.out.printf("input miners: %s, signature: %s\n", minerValue, signature);
        return Crypto.verify(Convert.parseHexString(signature), minerValue.getBytes(), Convert.parseHexString(publicKey), false);
    }

    @Override
    public Transaction parseTransaction(String transactionBytes, String transactionJSON) throws ParameterException {
        if (transactionBytes == null && transactionJSON == null) {
            throw new ParameterException(JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON);
        }
        if (transactionBytes != null) {
            try {
                byte[] bytes = Convert.parseHexString(transactionBytes);
                return transactionProcessor.parseTransaction(bytes);
            } catch (ValException.ValidationException | RuntimeException e) {
                logger.debug(e.getMessage(), e);
                JsonObject response = new JsonObject();
                response.addProperty(ResultFields.ERROR_CODE_RESPONSE, 4);
                response.addProperty(ResultFields.ERROR_DESCRIPTION_RESPONSE, "Incorrect transactionBytes: " + e.toString());
                throw new ParameterException(response);
            }
        } else {
            try {
                JsonObject json = JSON.getAsJsonObject(JSON.parse(transactionJSON));
                return transactionProcessor.parseTransaction(json);
            } catch (ValException.ValidationException | RuntimeException e) {
                logger.debug(e.getMessage(), e);
                JsonObject response = new JsonObject();
                response.addProperty(ResultFields.ERROR_CODE_RESPONSE, 4);
                response.addProperty(ResultFields.ERROR_DESCRIPTION_RESPONSE, "Incorrect transactionJSON: " + e.toString());
                throw new ParameterException(response);
            }
        }
    }

    @Override
    public AT getAT(HttpServletRequest req) throws ParameterException {
        String atValue = Convert.emptyToNull(req.getParameter(Parameters.AT_PARAMETER));
        if (atValue == null) {
            throw new ParameterException(JSONResponses.MISSING_AT);
        }
        AT at;
        try {
            Long atId = Convert.parseUnsignedLong(atValue);
            at = atService.getAT(atId);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONResponses.INCORRECT_AT);
        }
        if (at == null) {
            throw new ParameterException(JSONResponses.UNKNOWN_AT);
        }
        return at;
    }
}
