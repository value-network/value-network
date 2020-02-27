package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.DigitalGoodsStore;
import val.ValException;
import val.db.DbIterator;
import val.services.DGSGoodsStoreService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.PURCHASES_RESPONSE;

public final class GetDGSPendingPurchases extends APIServlet.APIRequestHandler {

    private final DGSGoodsStoreService dgsGoodStoreService;

    GetDGSPendingPurchases(DGSGoodsStoreService dgsGoodStoreService) {
        super(new APITag[]{APITag.DGS}, SELLER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
        this.dgsGoodStoreService = dgsGoodStoreService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long sellerId = ParameterParser.getSellerId(req);

        if (sellerId == 0) {
            return JSONResponses.MISSING_SELLER;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JsonObject response = new JsonObject();
        JsonArray purchasesJSON = new JsonArray();

        try (DbIterator<DigitalGoodsStore.Purchase> purchases = dgsGoodStoreService.getPendingSellerPurchases(sellerId, firstIndex, lastIndex)) {
            while (purchases.hasNext()) {
                purchasesJSON.add(JSONData.purchase(purchases.next()));
            }
        }

        response.add(PURCHASES_RESPONSE, purchasesJSON);
        return response;
    }

}
