package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class ImageProducer extends Thread {

    final private BioFormatsLoader loader;
    final private Executor executor;
    final private FeatureVectorFactory factory = new FeatureVectorFactory();
    private final Queue<FeatureVectorFactory.FeatureVector> resultsQueue;
    final private Logger log = LoggerFactory.getLogger(ImageProducer.class);
    public int count = 0;

    public ImageProducer(BioFormatsLoader loader, Executor executor, Queue<FeatureVectorFactory.FeatureVector> resultsQueue) {
        this.loader = loader;
        this.executor = executor;
        this.resultsQueue = resultsQueue;
    }

    @Override
    public void run() {

        int rejected = 0;
        while(this.loader.hasNext()) {
            BioFormatsImage image = loader.next();
            try {
                executor.execute(() -> {
                    FeatureVectorFactory.FeatureVector vec = factory.computeVector(image);
                    resultsQueue.add(vec);
                });
                count++;
            } catch (RejectedExecutionException e) {
                rejected++;
                log.error("Rejected task " + rejected);
            }
        }
        log.error("Encountered " + rejected + " rejected tasks");
    }
}
