package be.maximl.output;

import be.maximl.feature.FeatureVectorFactory;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.util.concurrent.CompletionService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FeatureVecWriter extends Thread {
    protected final LogService log;
    protected final AtomicInteger handleCount = new AtomicInteger();
    protected final StatusService statusService;
    protected final CompletionService<FeatureVectorFactory.FeatureVector> completionService;

    public FeatureVecWriter(LogService log, StatusService statusService, CompletionService<FeatureVectorFactory.FeatureVector> completionService) {
        this.log = log;
        this.statusService = statusService;
        this.completionService = completionService;
    }

    @Override
    public abstract void run();

    public AtomicInteger getHandled() {
        return handleCount;
    }
}
