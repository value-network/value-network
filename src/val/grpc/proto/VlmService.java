package val.grpc.proto;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import val.Blockchain;
import val.BlockchainProcessor;
import val.Generator;
import val.TransactionProcessor;
import val.grpc.GrpcApiHandler;
import val.grpc.handlers.*;
import val.services.AccountService;
import val.services.BlockService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VlmService extends VlmApiServiceGrpc.VlmApiServiceImplBase {

    private final Map<Class<? extends GrpcApiHandler<?, ?>>, GrpcApiHandler<?, ?>> handlers;

    public VlmService(BlockchainProcessor blockchainProcessor, Blockchain blockchain, BlockService blockService, AccountService accountService, Generator generator, TransactionProcessor transactionProcessor) {
        Map<Class<? extends GrpcApiHandler<?, ?>>, GrpcApiHandler<?, ?>> handlerMap = new HashMap<>();
        handlerMap.put(GetMiningInfoHandler.class, new GetMiningInfoHandler(blockchainProcessor));
        handlerMap.put(SubmitNonceHandler.class, new SubmitNonceHandler(blockchain, accountService, generator));
        handlerMap.put(GetBlockHandler.class, new GetBlockHandler(blockchain, blockService));
        handlerMap.put(GetAccountHandler.class, new GetAccountHandler(accountService));
        handlerMap.put(GetAccountsHandler.class, new GetAccountsHandler(accountService));
        handlerMap.put(GetTransactionHandler.class, new GetTransactionHandler(blockchain, transactionProcessor));
        handlerMap.put(GetTransactionBytesHandler.class, new GetTransactionBytesHandler(blockchain, transactionProcessor));
        this.handlers = Collections.unmodifiableMap(handlerMap);
    }

    private <T extends GrpcApiHandler<?, ?>> T getHandler(Class<T> handlerClass) throws HandlerNotFoundException {
        GrpcApiHandler<?, ?> handler = handlers.get(handlerClass);
        if (!handlerClass.isInstance(handler)) {
            throw new HandlerNotFoundException();
        }
        return handlerClass.cast(handler);
    }

    @Override
    public void getMiningInfo(Empty request, StreamObserver<VlmApi.MiningInfo> responseObserver) {
        try {
            getHandler(GetMiningInfoHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void submitNonce(VlmApi.SubmitNonceRequest request, StreamObserver<VlmApi.SubmitNonceResponse> responseObserver) {
        try {
            getHandler(SubmitNonceHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAccount(VlmApi.GetAccountRequest request, StreamObserver<VlmApi.Account> responseObserver) {
        try {
            getHandler(GetAccountHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAccounts(VlmApi.GetAccountsRequest request, StreamObserver<VlmApi.Accounts> responseObserver) {
        try {
            getHandler(GetAccountsHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getBlock(VlmApi.GetBlockRequest request, StreamObserver<VlmApi.Block> responseObserver) {
        try {
            getHandler(GetBlockHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTransaction(VlmApi.GetTransactionRequest request, StreamObserver<VlmApi.Transaction> responseObserver) {
        try {
            getHandler(GetTransactionHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTransactionBytes(VlmApi.GetTransactionRequest request, StreamObserver<VlmApi.TransactionBytes> responseObserver) {
        try {
            getHandler(GetTransactionBytesHandler.class).handleRequest(request, responseObserver);
        } catch (HandlerNotFoundException e) {
            responseObserver.onError(e);
        }
    }

    private class HandlerNotFoundException extends Exception {
    }
}
