package val.http;

import com.google.gson.JsonElement;
import val.ValException;
import val.services.AccountService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.AT_PARAMETER;

class GetATDetails extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;

    GetATDetails(ParameterService parameterService, AccountService accountService) {
        super(new APITag[]{APITag.AT}, AT_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        return JSONData.at(parameterService.getAT(req), accountService);
    }
}
