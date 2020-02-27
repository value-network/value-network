package val.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.*;
import val.Account.Pledges;
import val.crypto.Crypto;
import val.db.DbIterator;
import val.services.AccountService;
import val.services.BlockService;
import val.services.TransactionService;
import val.util.Convert;
import val.util.DownloadCacheImpl;
import val.util.ThreadPool;

import java.math.BigInteger;
import java.util.Arrays;


public class BlockServiceImpl implements BlockService {

    private static final Logger logger = LoggerFactory.getLogger(BlockServiceImpl.class);
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final Blockchain blockchain;
    private final DownloadCacheImpl downloadCache;
    private final Generator generator;

    public BlockServiceImpl(AccountService accountService, TransactionService transactionService, Blockchain blockchain, DownloadCacheImpl downloadCache, Generator generator) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.blockchain = blockchain;
        this.downloadCache = downloadCache;
        this.generator = generator;
    }

    @Override
    public boolean verifyBlockSignature(Block block) throws BlockchainProcessor.BlockOutOfOrderException {
        try {
            Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException(
                        "Can't verify signature because previous block is missing");
            }

            byte[] data = block.getBytes();
            byte[] data2 = new byte[data.length - 64];
            System.arraycopy(data, 0, data2, 0, data2.length);

            byte[] publicKey;
            Account genAccount = accountService.getAccount(block.getGeneratorPublicKey());

            if (null == genAccount) { // pool forge block
                if (!MinePool.getInstance().verifyMinePooler(block.getGeneratorId())) {
                    logger.info("only pool can forge block when account not exist");
                    return false;
                }
                publicKey = MinePool.getInstance().getPublicKey(block.getGeneratorId());
            } else { // user forge block
                Account.Pledges pledge;
                pledge = accountService.getAccountPledge(genAccount.getId());
                if (null == pledge) { // user is pool
                    if (!MinePool.getInstance().verifyMinePooler(genAccount.getId())) {
                        logger.info("only pool can forge block without pledge");
                        return false;
                    }
                    publicKey = genAccount.getPublicKey();
                    if (null == publicKey) {
                        publicKey = MinePool.getInstance().getPublicKey(genAccount.getId());
                    }
                } else { // normal pledge user
                    Account poolAccount = accountService.getAccount(pledge.getRecipID());
                    if (null == poolAccount || null == poolAccount.getPublicKey()) {
                        if (!MinePool.getInstance().verifyMinePooler(pledge.getRecipID())) {
                            logger.info("only pool can forge block without pledge");
                            return false;
                        }
                        publicKey = MinePool.getInstance().getPublicKey(pledge.getRecipID());
                    } else {
                        publicKey = poolAccount.getPublicKey();
                    }
                }
            }

            if (null == publicKey) {
                logger.info("invalid publicKey");
                return false;
            }

            // logger.info("block verfy sign, id:[{}], key:[{}], sign:[{}], data:[{}]", block.getId(), Convert.toHexString(publicKey), Convert.toHexString(block.getBlockSignature()), Convert.toHexString(data2));
            return Crypto.verify(block.getBlockSignature(), data2, publicKey, block.getVersion() >= 3);
        } catch (RuntimeException e) {
            logger.info("Error verifying block signature", e);
            return false;
        }

    }

    @Override
    public boolean verifyGenerationSignature(final Block block) throws BlockchainProcessor.BlockNotAcceptedException {
        try {
            Block previousBlock = blockchain.getBlock(block.getPreviousBlockId());

            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException(
                        "Can't verify generation signature because previous block is missing");
            }

            byte[] correctGenerationSignature = generator.calculateGenerationSignature(
                    previousBlock.getGenerationSignature(), previousBlock.getGeneratorId());
            if (!Arrays.equals(block.getGenerationSignature(), correctGenerationSignature)) {
                return false;
            }
            int elapsedTime = block.getTimestamp() - previousBlock.getTimestamp();
            BigInteger pTime = block.getPocTime().divide(BigInteger.valueOf(previousBlock.getBaseTarget()));
            return BigInteger.valueOf(elapsedTime).compareTo(pTime) > 0;
        } catch (RuntimeException e) {
            logger.info("Error verifying block generation signature", e);
            return false;
        }
    }

    @Override
    public void preVerify(Block block) throws BlockchainProcessor.BlockNotAcceptedException, InterruptedException {
        preVerify(block, null);
    }

    @Override
    public void preVerify(Block block, byte[] scoopData) throws BlockchainProcessor.BlockNotAcceptedException, InterruptedException {
        // Just in case its already verified
        if (block.isVerified()) {
            return;
        }

        try {
            // Pre-verify poc, calculate pocTime
            if (scoopData == null) {
                block.setPocTime(generator.calculateHit(block.getGeneratorId(), block.getNonce(), block.getGenerationSignature(), getScoopNum(block), block.getHeight()));
            } else {
                block.setPocTime(generator.calculateHit(block.getGeneratorId(), block.getNonce(), block.getGenerationSignature(), scoopData));
            }
        } catch (RuntimeException e) {
            logger.info("Error pre-verifying block generation signature", e);
            return;
        }

        for (Transaction transaction : block.getTransactions()) {
            if (!transaction.verifySignature()) {
                logger.info("Bad transaction signature during block pre-verification for tx: {} at block height: {}",
                        Convert.toUnsignedLong(transaction.getId()), block.getHeight());
                throw new BlockchainProcessor.TransactionNotAcceptedException("Invalid signature for tx: "
                        + Convert.toUnsignedLong(transaction.getId()) + " at block height: " + block.getHeight(),
                        transaction);
            }
            if (Thread.currentThread().isInterrupted() || !ThreadPool.running.get())
                throw new InterruptedException();
        }

    }

    @Override
    public void apply(Block block) {


        applyPledgeForgeReward(block);

    /*
    Account generatorAccount = accountService.getOrAddAccount(block.getGeneratorId());
    Account foundationAccount = accountService.getOrAddAccount(Account.getId(Constants.FOUNDATION_PUBLIC_KEY));
    generatorAccount.apply(block.getGeneratorPublicKey(), block.getHeight());

    long totalAmountNQT = Convert.safeAdd(block.getTotalFeeNQT(), getBlockReward(block));
    long foundationAmountNQT = Convert.safeDivide(Convert.safeMultiply(totalAmountNQT, Constants.BLOCK_REWARD_FOUNDATION_PERCENT), Constants.BLOCK_REWARD_TOTAL_PERCENT);

    if (block.getHeight() == 1) {
      foundationAmountNQT = 0L;
    }
    long minerAmountNQT = Convert.safeSubtract(totalAmountNQT, foundationAmountNQT);

    if (!Volume.getFluxCapacitor().isActive(FeatureToggle.REWARD_RECIPIENT_ENABLE)) {
      accountService.addToBalanceAndUnconfirmedBalanceNQT(generatorAccount, minerAmountNQT);
      accountService.addToForgedBalanceNQT(generatorAccount, minerAmountNQT);
      accountService.addToBalanceAndUnconfirmedBalanceNQT(foundationAccount, foundationAmountNQT);
      accountService.addToForgedBalanceNQT(foundationAccount, foundationAmountNQT);
    } else {
      Account rewardAccount;
      Account.RewardRecipientAssignment rewardAssignment = accountService.getRewardRecipientAssignment(generatorAccount);
      if (rewardAssignment == null) {
        rewardAccount = generatorAccount;
      } else if (block.getHeight() >= rewardAssignment.getFromHeight()) {
        rewardAccount = accountService.getAccount(rewardAssignment.getRecipientId());
      } else {
        rewardAccount = accountService.getAccount(rewardAssignment.getPrevRecipientId());
      }
      accountService.addToBalanceAndUnconfirmedBalanceNQT(rewardAccount, minerAmountNQT);
      accountService.addToForgedBalanceNQT(rewardAccount, minerAmountNQT);
      accountService.addToBalanceAndUnconfirmedBalanceNQT(foundationAccount, foundationAmountNQT);
      accountService.addToForgedBalanceNQT(foundationAccount, foundationAmountNQT);
    }
    */


        for (Transaction transaction : block.getTransactions()) {
            transactionService.apply(transaction);
        }
    }

    public void applyPledgeForgeReward(Block block) {
        Account generatorAccount = accountService.getOrAddAccount(block.getGeneratorId());
        Account foundationAccount = accountService.getOrAddAccount(Account.getId(Convert.parseHexString(Constants.FOUNDATION_MINER_KEY_HEX)));
        generatorAccount.apply(block.getGeneratorPublicKey(), block.getHeight());

        Account.Pledges generatorPledge = accountService.getAccountPledge(generatorAccount.getId());
        Account poolAccount;
        if (null == generatorPledge) {
            poolAccount = generatorAccount;
        } else {
            poolAccount = accountService.getAccount(generatorPledge.getRecipID());
        }

        long forgedReward = getBlockReward(block);
        block.setForgeReward(forgedReward);

        long totalForgeReward = Convert.safeAdd(forgedReward, block.getTotalFeeNQT());
        long totalForgeRewardLeft = totalForgeReward;

        long foundationReward = Convert.safeDivide(Convert.safeMultiply(totalForgeRewardLeft, Constants.BLOCK_REWARD_FOUNDATION_PERCENT), Constants.BLOCK_REWARD_FOUNDATION_PERCENT_BASE);
        // System.out.printf("foundation account:%d get %d\n", foundationAccount.getId(), foundationReward);

        long poolReward = Convert.safeDivide(Convert.safeMultiply(totalForgeRewardLeft, Constants.BLOCK_REWARD_MINER_PERCENT), Constants.BLOCK_REWARD_FOUNDATION_PERCENT_BASE);
        // System.out.printf("foundation account:%d get %d\n", foundationAccount.getId(), foundationReward);

        // 剩余奖励分给质押用户
        totalForgeRewardLeft = Convert.safeSubtract(totalForgeRewardLeft, foundationReward);
        totalForgeRewardLeft = Convert.safeSubtract(totalForgeRewardLeft, poolReward);

        long tiny = calcPoolMinerReward(poolAccount, totalForgeRewardLeft, block);
        logger.info("block:[{}->{}], tiny:[{}], totalReward:[{}], foundationReward:[{}], mateRewardAll:[{}]",
                block.getHeight(), block.getId(), tiny, totalForgeReward, foundationReward, totalForgeRewardLeft);
        accountService.addToBalanceAndUnconfirmedBalanceNQT(foundationAccount, foundationReward + tiny);
        accountService.addToForgedBalanceNQT(foundationAccount, foundationReward + tiny);
        accountService.addToBalanceAndUnconfirmedBalanceNQT(generatorAccount, poolReward);
        accountService.addToForgedBalanceNQT(generatorAccount, poolReward);

        // tiny 之前是销毁的，现在转给基金会
//        accountService.burningRemanentForgeReward(block, forgedReward - tiny); // make sure the total is stable
    }

    private long calcPoolMinerReward(Account poolAccount, long reward, Block block) {
        DbIterator<Pledges> iter = accountService.getPoolAllMinerPledge(poolAccount.getId());

        BigInteger a = new BigInteger(Convert.toUnsignedLong(reward));
        long totalPledged = poolAccount.getTotalPledged();
        if (0 == totalPledged) {
            return reward;
        }
        BigInteger b = new BigInteger(Convert.toUnsignedLong(totalPledged));

        long userPledgedSum = 0;
        int idx = 0;
        long sent = 0;

        while (iter.hasNext()) {
            idx += 1;
            Account.Pledges pledge = iter.next();
            Account account = accountService.getAccount(pledge.getAccountID());
            BigInteger userPledged = new BigInteger(Convert.toUnsignedLong(pledge.getPledgeTotal()));
            // long accountReward = Convert.safeDivide(Convert.safeMultiply(reward, pledge.getPledgeTotal()), totalPledged);
            long accountReward = userPledged.multiply(a).divide(b).longValue();
            sent += accountReward;
            accountService.addToBalanceAndUnconfirmedBalanceNQT(account, accountReward);
            accountService.addToPledgeRewardBalance(account, accountReward);
            userPledgedSum += pledge.getPledgeTotal();
            // logger.info("calcPoolMinerReward block:[{}->{}], user total pledged sum:[{}], user index:[{}]: account:[{}], pledged:[{}], reward:[{}], totalPledged:[{}], totalReward:[{}]",
            //   block.getHeight(), block.getId(), userPledgedSum, idx, account.getId(), pledge.getPledgeTotal(), accountReward, totalPledged, reward);
            // System.out.printf("'%d\t%d\n", account.getId(), accountReward);
        }
        return Convert.safeSubtract(reward, sent);
    }

    /*
     calculate forge block reward according to the pool current pledge total

     Deprecated
    */
    private long getBlockPledgeReward(Block block, Account poolAccount) {
        long totalPledged = poolAccount.getTotalPledged();

        long reward = MinePool.getInstance().getPledgeReward(totalPledged, block.getHeight());
        //System.out.printf("account:%s,totalPledged:%s,height:%s,reward:%s\n", poolAccount.id, totalPledged,block.getHeight(),reward);
        // if (totalPledged < 1024 * 5 * Constants.MIN_PLEDGE_1T) {
        //   logger.info("getBlockPledgeReward, block:[{}->{}], account:[{}], totalPledged:[{}], reward:[{}]", block.getHeight(), block.getId(), poolAccount.getId(), totalPledged, reward);
        //   return 0;
        // }

        // // 1024 * 5 = 5P, 1000 is maximum pledge amount, 2000 is max forge reward
        // BigInteger a = new BigInteger(Convert.toUnsignedLong(totalPledged));
        // BigInteger b = new BigInteger(Convert.toUnsignedLong(Constants.MAX_PLEDGE_REWARD));
        // BigInteger c = new BigInteger(Convert.toUnsignedLong(1024 * 5 * Constants.MAX_PLEDGE_1T));

        // reward = a.multiply(b).divide(c).longValue();
        // if (Constants.MAX_PLEDGE_REWARD < reward) {
        //   reward = Constants.MAX_PLEDGE_REWARD;
        // }

        // logger.info("getBlockPledgeReward, block:[{}->{}], account:[{}], totalPledged:[{}], reward:[{}]", block.getHeight(), block.getId(), poolAccount.getId(), totalPledged, reward);
        return reward;
    }

    @Override
    public long getBlockReward(Block block) {
        if (block.getHeight() <= 0 || block.getHeight() > 599678) {
            return 0L;
        }

        // 每 360 * 30 个块增长2%;
        int cycle = block.getHeight() / (360 * 30);
        long reward = (long) (Constants.BLOCK_REWARD_MONTH_BASE * Math.pow(1.1, cycle) /360/30);

        return reward;
    }

    @Override
    public void setPrevious(Block block, Block previousBlock) {
        if (previousBlock != null) {
            if (previousBlock.getId() != block.getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            block.setHeight(previousBlock.getHeight() + 1);
            if (block.getBaseTarget() == Constants.INITIAL_BASE_TARGET) {
                try {
                    this.calculateBaseTarget(block, previousBlock);
                } catch (BlockchainProcessor.BlockOutOfOrderException e) {
                    throw new IllegalStateException(e.toString(), e);
                }
            }
        } else {
            block.setHeight(0);
        }
        block.getTransactions().forEach(transaction -> transaction.setBlock(block));
    }

    @Override
    public void calculateBaseTarget(Block block, Block previousBlock) throws BlockchainProcessor.BlockOutOfOrderException {
        if (block.getId() == Genesis.GENESIS_BLOCK_ID && block.getPreviousBlockId() == 0) {
            block.setBaseTarget(Constants.INITIAL_BASE_TARGET);
            block.setCumulativeDifficulty(BigInteger.ZERO);
        } else if (block.getHeight() < 4) {
            block.setBaseTarget(Constants.INITIAL_BASE_TARGET);
            block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(Constants.INITIAL_BASE_TARGET))));
        } else if (block.getHeight() < Constants.CHAIN_DIFF_ADJUST_CHANGE_BLOCK) {
            Block itBlock = previousBlock;
            BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
            do {
                itBlock = downloadCache.getBlock(itBlock.getPreviousBlockId());
                avgBaseTarget = avgBaseTarget.add(BigInteger.valueOf(itBlock.getBaseTarget()));
            } while (itBlock.getHeight() > block.getHeight() - 4);
            avgBaseTarget = avgBaseTarget.divide(BigInteger.valueOf(4));
            long difTime = (long) block.getTimestamp() - itBlock.getTimestamp();

            long curBaseTarget = avgBaseTarget.longValue();
            long newBaseTarget = BigInteger.valueOf(curBaseTarget).multiply(BigInteger.valueOf(difTime))
                    .divide(BigInteger.valueOf(240L * 4)).longValue();
            if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
                newBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget < (curBaseTarget * 9 / 10)) {
                newBaseTarget = curBaseTarget * 9 / 10;
            }
            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }
            long twofoldCurBaseTarget = curBaseTarget * 11 / 10;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget > twofoldCurBaseTarget) {
                newBaseTarget = twofoldCurBaseTarget;
            }
            block.setBaseTarget(newBaseTarget);
            block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(newBaseTarget))));
        } else {
            Block itBlock = previousBlock;
            BigInteger avgBaseTarget = BigInteger.valueOf(itBlock.getBaseTarget());
            int blockCounter = 1;
            do {
                int previousHeight = itBlock.getHeight();
                itBlock = downloadCache.getBlock(itBlock.getPreviousBlockId());
                if (itBlock == null) {
                    throw new BlockchainProcessor.BlockOutOfOrderException("Previous block does no longer exist for block height " + previousHeight);
                }
                blockCounter++;
                avgBaseTarget = (avgBaseTarget.multiply(BigInteger.valueOf(blockCounter))
                        .add(BigInteger.valueOf(itBlock.getBaseTarget())))
                        .divide(BigInteger.valueOf(blockCounter + 1L));
            } while (blockCounter < 24);
            long difTime = (long) block.getTimestamp() - itBlock.getTimestamp();
            long targetTimespan = 24L * 4 * 60;

            if (difTime < targetTimespan / 2) {
                difTime = targetTimespan / 2;
            }

            if (difTime > targetTimespan * 2) {
                difTime = targetTimespan * 2;
            }

            long curBaseTarget = previousBlock.getBaseTarget();
            long newBaseTarget = avgBaseTarget.multiply(BigInteger.valueOf(difTime))
                    .divide(BigInteger.valueOf(targetTimespan)).longValue();

            if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
                newBaseTarget = Constants.MAX_BASE_TARGET;
            }

            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }

            if (newBaseTarget < curBaseTarget * 8 / 10) {
                newBaseTarget = curBaseTarget * 8 / 10;
            }

            if (newBaseTarget > curBaseTarget * 12 / 10) {
                newBaseTarget = curBaseTarget * 12 / 10;
            }

            block.setBaseTarget(newBaseTarget);
            block.setCumulativeDifficulty(previousBlock.getCumulativeDifficulty().add(Convert.two64.divide(BigInteger.valueOf(newBaseTarget))));
        }
    }

    @Override
    public int getScoopNum(Block block) {
        return generator.calculateScoop(block.getGenerationSignature(), block.getHeight());
    }

    public String getNextCumulativeDifficulty(Block block) throws ValException.ValidationException, BlockchainProcessor.BlockOutOfOrderException {
        int nextTime = block.getTimestamp() + 4 * 60;
        long previousBlockId = block.getId();
        Block nextBlock = new Block(-1, nextTime, previousBlockId, 0L, 0L, 0, new byte[32], new byte[32], new byte[32], new byte[32], new byte[32], null, 0L, block.getId(), block.getHeight() + 1, 2L, 0L, new byte[32]);
        // logger.info("getNextCumulativeDifficulty, blockID:[{} vs {}], blockTime:[{} vs {}], blockHeight:[{}->{}]", nextBlock.getPreviousBlockId(), nextBlock.getId(), block.getTimestamp(), nextBlock.getTimestamp(), block.getHeight(),nextBlock.getHeight());
        calculateBaseTarget(nextBlock, block);
        logger.info("getNextCumulativeDifficulty, difficulty:[{}->{}]", block.getCumulativeDifficulty(), nextBlock.getCumulativeDifficulty());
        return nextBlock.getCumulativeDifficulty().toString();
    }

}
