package val.http;

import com.google.gson.JsonElement;
import val.ValException;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.PURCHASE_PARAMETER;

public final class GetDGSPurchase extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;

    public GetDGSPurchase(ParameterService parameterService) {
        super(new APITag[]{APITag.DGS}, PURCHASE_PARAMETER);
        this.parameterService = parameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        return JSONData.purchase(parameterService.getPurchase(req));
    }

}
