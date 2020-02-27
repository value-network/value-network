package val.services.impl;

import val.services.TimeService;
import val.util.Time;

import java.util.concurrent.atomic.AtomicReference;

public class TimeServiceImpl implements TimeService {

    private static final AtomicReference<Time> time = new AtomicReference<>(new Time.EpochTime());

    @Override
    public int getEpochTime() {
        return time.get().getTime();
    }

    @Override
    public long getEpochTimeMillis() {
        return time.get().getTimeInMillis();
    }

    @Override
    public void setTime(Time.FasterTime t) {
        time.set(t);
    }

}
