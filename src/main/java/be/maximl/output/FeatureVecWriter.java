package be.maximl.output;

import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class FeatureVecWriter extends Thread {
    protected final LogService log;
    protected final AtomicInteger handleCount = new AtomicInteger();
    protected final StatusService statusService;

    public FeatureVecWriter(LogService log, StatusService statusService) {
        this.log = log;
        this.statusService = statusService;
    }

    public AtomicInteger getHandled() {
        return handleCount;
    }
}
