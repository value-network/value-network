package val.grpc.proto;

import com.google.protobuf.ByteString;
import val.*;
import val.db.DbIterator;
import val.services.AccountService;
import val.services.BlockService;
import val.util.Convert;

import java.util.stream.Collectors;

public final class ProtoBuilder {

    private ProtoBuilder() {
    }

    public static VlmApi.Account buildAccount(Account account, AccountService accountService) {
        VlmApi.Account.Builder builder = VlmApi.Account.newBuilder()
                .setId(account.getId())
                .setPublicKey(ByteString.copyFrom(account.getPublicKey()))
                .setBalance(account.getBalanceNQT())
                .setUnconfirmedBalance(account.getUnconfirmedBalanceNQT())
                .setForgedBalance(account.getForgedBalanceNQT())
                .setName(account.getName())
                .setDescription(account.getDescription())
                .setRewardRecipient(accountService.getRewardRecipientAssignment(account).accountId);

        try (DbIterator<Account.AccountAsset> assets = accountService.getAssets(account.id, 0, -1)) {
            assets.forEachRemaining(asset -> builder.addAssetBalances(buildAssetBalance(asset)));
        }

        return builder.build();
    }

    private static VlmApi.AssetBalance buildAssetBalance(Account.AccountAsset asset) {
        return VlmApi.AssetBalance.newBuilder()
                .setId(asset.getAssetId())
                .setBalance(asset.getQuantityQNT())
                .setUnconfirmedBalance(asset.getUnconfirmedQuantityQNT())
                .build();
    }

    public static VlmApi.Block buildBlock(Blockchain blockchain, BlockService blockService, Block block, boolean includeTransactions) {
        VlmApi.Block.Builder builder = VlmApi.Block.newBuilder()
                .setId(block.getId())
                .setHeight(block.getHeight())
                .setNumberOfTransactions(block.getTransactions().size())
                .setTotalAmount(block.getTotalAmountNQT())
                .setTotalFee(block.getTotalFeeNQT())
                .setBlockReward(block.getForgeReward())
                .setPayloadLength(block.getPayloadLength())
                .setVersion(block.getVersion())
                .setBaseTarget(block.getBaseTarget())
                .setTimestamp(block.getTimestamp())
                .setGenerationSignature(ByteString.copyFrom(block.getGenerationSignature()))
                .setBlockSignature(ByteString.copyFrom(block.getBlockSignature()))
                .setPayloadHash(ByteString.copyFrom(block.getPayloadHash()))
                .setGeneratorPublicKey(ByteString.copyFrom(block.getGeneratorPublicKey()))
                .setNonce(block.getNonce())
                .setScoop(blockService.getScoopNum(block))
                .setPreviousBlock(block.getPreviousBlockId())
                .setNextBlock(block.getNextBlockId())
                .setPreviousBlockHash(ByteString.copyFrom(block.getPreviousBlockHash()));

        if (includeTransactions) {
            builder.addAllTransactions(block.getTransactions().stream()
                    .map(transaction -> buildTransaction(blockchain, transaction))
                    .collect(Collectors.toList()));
        } else {
            builder.addAllTransactionIds(block.getTransactions().stream()
                    .map(Transaction::getId)
                    .collect(Collectors.toList()));
        }
        return builder.build();
    }

    public static VlmApi.Transaction buildTransaction(Blockchain blockchain, Transaction transaction) {
        VlmApi.Transaction.Builder builder = VlmApi.Transaction.newBuilder()
                .setId(transaction.getId())
                .setVersion(transaction.getVersion())
                .setType(transaction.getType().getType())
                .setSubtype(transaction.getType().getSubtype())
                .setTimestamp(transaction.getTimestamp())
                .setDeadline(transaction.getDeadline())
                .setSender(ByteString.copyFrom(transaction.getSenderPublicKey()))
                .setRecipient(transaction.getRecipientId())
                .setAmount(transaction.getAmountNQT())
                .setFee(transaction.getFeeNQT())
                .setBlock(transaction.getBlockId())
                .setBlockHeight(transaction.getHeight())
                .setBlockTimestamp(transaction.getBlockTimestamp())
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .setReferencedTransactionFullHash(ByteString.copyFrom(Convert.parseHexString(transaction.getReferencedTransactionFullHash())))
                .setFullHash(ByteString.copyFrom(Convert.parseHexString(transaction.getFullHash())))
                .setConfirmations(blockchain.getHeight() - transaction.getHeight())
                .setEcBlockId(transaction.getECBlockId())
                .setEcBlockHeight(transaction.getECBlockHeight())
                .addAllAppendices(transaction.getAppendages().stream()
                        .map(Appendix::getProtobufMessage)
                        .collect(Collectors.toList()));

        return builder.build();
    }
}
