package val.grpc.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Account;
import val.Blockchain;
import val.Generator;
import val.crypto.Crypto;
import val.grpc.GrpcApiHandler;
import val.grpc.proto.ApiException;
import val.grpc.proto.VlmApi;
import val.services.AccountService;
import val.util.Convert;

import java.util.Objects;

public class SubmitNonceHandler implements GrpcApiHandler<VlmApi.SubmitNonceRequest, VlmApi.SubmitNonceResponse> {

    private static final Logger logger = LoggerFactory.getLogger(SubmitNonceHandler.class);

    private final Blockchain blockchain;
    private final AccountService accountService;
    private final Generator generator;

    public SubmitNonceHandler(Blockchain blockchain, AccountService accountService, Generator generator) {
        this.blockchain = blockchain;
        this.accountService = accountService;
        this.generator = generator;
    }

    /**
     * 如果 accountId 不是0，使用 accountId获取用户信息 -> genAccount
     * 否则 genAccount = secretAccount
     * <p>
     * 使用 genAccount 查询 rewardRecipientAssignment: 貌似是出块奖励的收入分配凭证
     * rewardId 为最终的收益人Id
     * 如果 rewardRecipientAssignment 没有，则收益给 genAccount
     * 如果 rewardRecipientAssignment 非空，且起FromHeight > 当前待出块的大小，则从 assignment 中获取 PrevReceipientId 作为奖励的接收者
     * 否则 rewardId 为 assignment 中指定的 RecipientId
     * 如果 rewardId 和 genAccount.Id 应该一致，否则报错
     * <p>
     * <p>
     * 问题:
     * 1. secretAccount 和 accountId 的关系
     * 2. RewardRecipientAssignment 的作用，其中包含的字段含义
     * 1. FromHeight
     * 2. PrevRecipientId
     * 3. RecipientId
     *
     * @param accountService
     * @param blockchain
     * @param secretAccount  secretAccount 账户
     * @param accountId      出矿用户ID
     * @throws ApiException
     */
    public static void verifySecretAccount(AccountService accountService, Blockchain blockchain, Account secretAccount, long accountId) throws ApiException {
        Account genAccount;
        if (accountId != 0) {
            genAccount = accountService.getAccount(accountId);
        } else {
            genAccount = secretAccount;
        }
        logger.info("accountId:{}, secretAccount.id:{}, genAccount is null:{}", accountId, secretAccount.id, genAccount == null);

        if (genAccount != null) {
            Account.RewardRecipientAssignment assignment = accountService.getRewardRecipientAssignment(genAccount);
            long rewardId;
            if (assignment == null) {
                rewardId = genAccount.getId();
            } else if (assignment.getFromHeight() > blockchain.getLastBlock().getHeight() + 1) {
                rewardId = assignment.getPrevRecipientId();
            } else {
                rewardId = assignment.getRecipientId();
            }
            if (rewardId != secretAccount.getId()) {
                throw new ApiException("Passphrase does not match reward recipient");
            }
        } else {
            throw new ApiException("Passphrase is for a different account");
        }
    }

    public static void verifyMinerPoolRelation(AccountService accountService, Blockchain blockchain, Account secretAccount, long accountId) throws ApiException {
        Account genAccount;
        if (accountId != 0) {
            genAccount = accountService.getAccount(accountId);
        } else {
            logger.info("pool forge block request");
            return;
        }

        //verify pool_miner
//        long poolId = MinePool.getInstance().getPoolAccountID();
//        if (poolId != accountId){
//        	Account.PoolMiner poolMiner = accountService.getGrantPoolMiner(accountId,poolId);
//            if (poolMiner == null){
//            	logger.error("pool: "+poolId+" need grant the miner: "+ accountId);
//            	throw new ApiException("pool: "+poolId+" need grant the miner: "+ accountId);
//            }
//        }


        logger.info("accountId:{}, pool id:{}", accountId, secretAccount.id);

        if (genAccount != null) {
            if (genAccount.getId() == secretAccount.getId()) {
                return;
            }

            Account.Pledges pledges = accountService.getAccountPledge(accountId);
            if (pledges == null) {
                throw new ApiException("miner need join pool");
            }

            if (pledges.getRecipID() != secretAccount.getId()) {
                throw new ApiException("Pledge recipient not match");
            }
        } else {
            throw new ApiException("Passphrase is for a different account");
        }
    }

    @Override
    public VlmApi.SubmitNonceResponse handleRequest(VlmApi.SubmitNonceRequest request) throws Exception {
        String secret = request.getSecretPhrase();
        long nonce = request.getNonce();
        long accountId = request.getAccount();
        int submissionHeight = request.getBlockHeight();

        if (submissionHeight != 0 && submissionHeight != blockchain.getHeight() + 1) {
            throw new ApiException("Given block height does not match current blockchain height");
        }

        if (Objects.equals(secret, "")) {
            throw new ApiException("Missing Passphrase");
        }

        byte[] secretPublicKey = Crypto.getPublicKey(secret);
        logger.info("accountId:[{}], nonce:[{}], secret:[{}], secret publicKey:[{}]", accountId, nonce, secret, Convert.toHexString(secretPublicKey));
        Account secretAccount = accountService.getAccount(secretPublicKey);
        if (secretAccount != null) {
            verifySecretAccount(accountService, blockchain, secretAccount, accountId);
        }

        Generator.GeneratorState generatorState;
        if (accountId == 0 || secretAccount == null) {
            generatorState = generator.addNonce(secret, nonce);
        } else {
            Account genAccount = accountService.getAccount(accountId);
            if (genAccount == null || genAccount.getPublicKey() == null) {
                throw new ApiException("Passthrough mining requires public key in blockchain");
            } else {
                byte[] publicKey = genAccount.getPublicKey();
                generatorState = generator.addNonce(secret, nonce, publicKey);
            }
        }

        if (generatorState == null) {
            throw new ApiException("Failed to create generator");
        }

        return VlmApi.SubmitNonceResponse.newBuilder().setDeadline(generatorState.getDeadline().longValueExact()).build();
    }

}
