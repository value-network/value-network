package val.http;

import com.google.gson.JsonElement;
import val.Order;
import val.ValException;
import val.assetexchange.AssetExchange;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ORDER_PARAMETER;

public final class GetAskOrder extends APIServlet.APIRequestHandler {

    private final AssetExchange assetExchange;

    GetAskOrder(AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, ORDER_PARAMETER);
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long orderId = ParameterParser.getOrderId(req);
        Order.Ask askOrder = assetExchange.getAskOrder(orderId);
        if (askOrder == null) {
            return JSONResponses.UNKNOWN_ORDER;
        }
        return JSONData.askOrder(askOrder);
    }

}
