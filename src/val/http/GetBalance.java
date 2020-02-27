package val.http;

import com.google.gson.JsonElement;
import val.ValException;
import val.http.common.Parameters;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

public final class GetBalance extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;

    public GetBalance(ParameterService parameterService) {
        super(new APITag[]{APITag.ACCOUNTS}, Parameters.ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        return JSONData.accountBalance(parameterService.getAccount(req));
    }

}
