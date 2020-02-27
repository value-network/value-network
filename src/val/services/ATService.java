package val.services;

import val.AT;

import java.util.Collection;
import java.util.List;

public interface ATService {

    Collection<Long> getAllATIds();

    List<Long> getATsIssuedBy(Long accountId);

    AT getAT(Long atId);
}
