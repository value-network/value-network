package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Order;
import val.assetexchange.AssetExchange;
import val.db.DbIterator;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static val.http.common.Parameters.LAST_INDEX_PARAMETER;

public final class GetAllOpenBidOrders extends APIServlet.APIRequestHandler {

    private final AssetExchange assetExchange;

    GetAllOpenBidOrders(AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        JsonObject response = new JsonObject();
        JsonArray ordersData = new JsonArray();

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        try (DbIterator<Order.Bid> bidOrders = assetExchange.getAllBidOrders(firstIndex, lastIndex)) {
            while (bidOrders.hasNext()) {
                ordersData.add(JSONData.bidOrder(bidOrders.next()));
            }
        }

        response.add("openOrders", ordersData);
        return response;
    }

}
