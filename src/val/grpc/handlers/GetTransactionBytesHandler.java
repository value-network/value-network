package val.grpc.handlers;

import com.google.protobuf.ByteString;
import val.Blockchain;
import val.TransactionProcessor;
import val.grpc.GrpcApiHandler;
import val.grpc.proto.VlmApi;

public class GetTransactionBytesHandler implements GrpcApiHandler<VlmApi.GetTransactionRequest, VlmApi.TransactionBytes> {

    private final Blockchain blockchain;
    private final TransactionProcessor transactionProcessor;

    public GetTransactionBytesHandler(Blockchain blockchain, TransactionProcessor transactionProcessor) {
        this.blockchain = blockchain;
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    public VlmApi.TransactionBytes handleRequest(VlmApi.GetTransactionRequest request) throws Exception {
        return VlmApi.TransactionBytes.newBuilder()
                .setTransactionBytes(ByteString.copyFrom(GetTransactionHandler.getTransaction(blockchain, transactionProcessor, request).getBytes()))
                .build();
    }
}
