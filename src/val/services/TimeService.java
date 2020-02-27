package val.services;

import val.util.Time;

public interface TimeService {

    int getEpochTime();

    long getEpochTimeMillis();

    void setTime(Time.FasterTime fasterTime);
}
