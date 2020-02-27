package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.*;
import val.grpc.handlers.SubmitNonceHandler;
import val.grpc.proto.ApiException;
import val.services.AccountService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;


final class SubmitNonce extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(SubmitNonce.class);

    private static final String SUBMIT_NONCE_GET_REQUEST = "submitNonce get request:{}";
    private final AccountService accountService;
    private final Blockchain blockchain;
    private final Generator generator;

    SubmitNonce(AccountService accountService, Blockchain blockchain, Generator generator) {
        super(new APITag[]{APITag.MINING}, SECRET_PHRASE_PARAMETER, NONCE_PARAMETER, ACCOUNT_ID_PARAMETER,
                BLOCK_HEIGHT_PARAMETER);

        this.accountService = accountService;
        this.blockchain = blockchain;
        this.generator = generator;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        JsonObject response = new JsonObject();
//        String minerVersion = req.getHeader("version");
//         Version minerVer = Version.parse(minerVersion);
//         //System.out.printf("header in submitNonce:%s\n", minerVersion);
//         if (minerVer == null || !minerVer.isGreaterThanOrEqualTo(Constants.MINER_MIN_VERSION)) {
//             System.out.printf("header in submitNonce:%s\n", minerVersion);
//             response.addProperty("result",
//                     "Miner version should be higher than " + Constants.MINER_MIN_VERSION.toString());
//             return response;
//         }
        String secret = req.getParameter(SECRET_PHRASE_PARAMETER);
        // try {
        // secret = URLDecoder.decode(req.getParameter(SECRET_PHRASE_PARAMETER),
        // "UTF-8");
        // } catch(UnsupportedEncodingException e) {
        // }

        long nonce = Convert.parseUnsignedLong(req.getParameter(NONCE_PARAMETER));

        String accountId = req.getParameter(ACCOUNT_ID_PARAMETER);

        String submissionHeight = Convert.emptyToNull(req.getParameter(BLOCK_HEIGHT_PARAMETER));

        if (!MinePool.getInstance().isPoolNode()) {
            response.addProperty("result", "Only pool node accept SubmitNonce");
            return response;
        }

        logger.info("submitNonce get request:{}", req.toString());

        if (submissionHeight != null) {
            try {
                int height = Integer.parseInt(submissionHeight);
                if (height != blockchain.getHeight() + 1) {
                    response.addProperty("result", "Given block height does not match current blockchain height");
                    return response;
                }
            } catch (NumberFormatException e) {
                response.addProperty("result", "Given block height is not a number");
                return response;
            }
        }

        if (secret != null && secret.length() > 0) {
            response.addProperty("result", "Do not accept Passphrase");
            return response;
        }

        // byte[] secretPublicKey = Crypto.getPublicKey(secret);
        // Account secretAccount = accountService.getAccount(secretPublicKey);
        secret = MinePool.getInstance().getPoolSecretPhrase();
        Account secretAccount = accountService.getAccount(MinePool.getInstance().getPoolAccountID());

        if (secretAccount != null) {
            try {
                SubmitNonceHandler.verifyMinerPoolRelation(accountService, blockchain, secretAccount,
                        Convert.parseUnsignedLong(accountId));
            } catch (ApiException e) {
                response.addProperty("result", e.getMessage());
                return response;
            }
        }

        Generator.GeneratorState generatorState = null;
        if (accountId == null || secretAccount == null || secretAccount.getId() == Convert.parseAccountId(accountId)) {
            generatorState = generator.addNonce(secret, nonce);
        } else {
            Account genAccount = accountService.getAccount(Convert.parseUnsignedLong(accountId));
            // logger.info("get genAccount using accountId:[{}]-[us:{}],
            // genAccount is null:[{}]",
            // accountId, Convert.parseUnsignedLong(accountId), genAccount ==
            // null);
            if (genAccount == null || genAccount.getPublicKey() == null) {
                response.addProperty("result", "Passthrough mining requires public key in blockchain");
                return response;
            }

            byte[] publicKey = genAccount.getPublicKey();
            logger.info("try to addNonce using account:[{}-unsigned ID:{}], publicKey:[{}], hexPublicKey:[{}]",
                    accountId, Convert.parseUnsignedLong(accountId), publicKey, Convert.toHexString(publicKey));
            generatorState = generator.addNonce(secret, nonce, publicKey);
            // logger.info("call addNonce({}, {}, {}), ret genStat is null:{}",
            // secret, nonce, Convert.toHexString(publicKey), generatorState ==
            // null);
        }

        if (generatorState == null) {
            response.addProperty("result", "failed to create generator");
            return response;
        }

        // response.addProperty("result", "deadline: " +
        // generator.getDeadline());
        response.addProperty("result", "success");
        response.addProperty("deadline", generatorState.getDeadline());

        return response;
    }

    @Override
    boolean requirePost() {
        return true;
    }
}
