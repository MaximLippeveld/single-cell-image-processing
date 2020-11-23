package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageProducer extends Thread {

    final private BioFormatsLoader loader;
    final private FeatureVectorFactory factory = new FeatureVectorFactory();
    private final CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private Logger log = LoggerFactory.getLogger(ImageProducer.class);
    final private AtomicInteger counter;

    public ImageProducer(BioFormatsLoader loader, CompletionService<FeatureVectorFactory.FeatureVector> completionService, AtomicInteger counter) {
        this.loader = loader;
        this.completionService = completionService;
        this.counter = counter;
    }

    @Override
    public void run() {

        int rejected = 0;
        while(this.loader.hasNext()) {
            BioFormatsImage image = loader.next();
            try {
                completionService.submit(() -> factory.computeVector(image));
                counter.incrementAndGet();
            } catch (RejectedExecutionException e) {
                rejected++;
                log.error("Rejected task " + rejected);
            }
        }
        log.error("Encountered " + rejected + " rejected tasks");
    }
}
