package val.http;

import com.google.gson.JsonElement;
import val.Order;
import val.ValException;
import val.assetexchange.AssetExchange;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ORDER_PARAMETER;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

    private final AssetExchange assetExchange;

    GetBidOrder(AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, ORDER_PARAMETER);
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long orderId = ParameterParser.getOrderId(req);
        Order.Bid bidOrder = assetExchange.getBidOrder(orderId);

        if (bidOrder == null) {
            return JSONResponses.UNKNOWN_ORDER;
        }

        return JSONData.bidOrder(bidOrder);
    }

}
