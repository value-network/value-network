package val.http;

import com.google.gson.JsonElement;
import val.Alias;
import val.http.common.Parameters;
import val.services.AliasService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

public final class GetAlias extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AliasService aliasService;

    GetAlias(ParameterService parameterService, AliasService aliasService) {
        super(new APITag[]{APITag.ALIASES}, Parameters.ALIAS_PARAMETER, Parameters.ALIAS_NAME_PARAMETER);
        this.parameterService = parameterService;
        this.aliasService = aliasService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ParameterException {
        final Alias alias = parameterService.getAlias(req);
        final Alias.Offer offer = aliasService.getOffer(alias);

        return JSONData.alias(alias, offer);
    }

}
