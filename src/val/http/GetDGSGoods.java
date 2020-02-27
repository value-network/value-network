package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.DigitalGoodsStore;
import val.ValException;
import val.db.DbIterator;
import val.db.sql.DbUtils;
import val.http.common.Parameters;
import val.services.DGSGoodsStoreService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.GOODS_RESPONSE;

public final class GetDGSGoods extends APIServlet.APIRequestHandler {

    private final DGSGoodsStoreService digitalGoodsStoreService;

    public GetDGSGoods(DGSGoodsStoreService digitalGoodsStoreService) {
        super(new APITag[]{APITag.DGS}, SELLER_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, IN_STOCK_ONLY_PARAMETER);
        this.digitalGoodsStoreService = digitalGoodsStoreService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long sellerId = ParameterParser.getSellerId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean inStockOnly = !Parameters.isFalse(req.getParameter(IN_STOCK_ONLY_PARAMETER));

        JsonObject response = new JsonObject();
        JsonArray goodsJSON = new JsonArray();
        response.add(GOODS_RESPONSE, goodsJSON);

        DbIterator<DigitalGoodsStore.Goods> goods = null;
        try {
            if (sellerId == 0) {
                if (inStockOnly) {
                    goods = digitalGoodsStoreService.getGoodsInStock(firstIndex, lastIndex);
                } else {
                    goods = digitalGoodsStoreService.getAllGoods(firstIndex, lastIndex);
                }
            } else {
                goods = digitalGoodsStoreService.getSellerGoods(sellerId, inStockOnly, firstIndex, lastIndex);
            }
            while (goods.hasNext()) {
                DigitalGoodsStore.Goods good = goods.next();
                goodsJSON.add(JSONData.goods(good));
            }
        } finally {
            DbUtils.close(goods);
        }

        return response;
    }

}
