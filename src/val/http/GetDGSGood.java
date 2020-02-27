package val.http;

import com.google.gson.JsonElement;
import val.ValException;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.GOODS_PARAMETER;

public final class GetDGSGood extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;

    GetDGSGood(ParameterService parameterService) {
        super(new APITag[]{APITag.DGS}, GOODS_PARAMETER);
        this.parameterService = parameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        return JSONData.goods(parameterService.getGoods(req));
    }

}
