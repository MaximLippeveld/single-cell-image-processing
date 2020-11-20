package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class ImageProducer extends Thread {

    final private BioFormatsLoader loader;
    final private ExecutorService executor;
    final private FeatureVectorFactory factory = new FeatureVectorFactory();
    private final Queue<FeatureVectorFactory.FeatureVector> resultsQueue;
    final private Logger log = LoggerFactory.getLogger(ImageProducer.class);
    public int count = 0;

    public ImageProducer(BioFormatsLoader loader, ExecutorService executor, Queue<FeatureVectorFactory.FeatureVector> resultsQueue) {
        this.loader = loader;
        this.executor = executor;
        this.resultsQueue = resultsQueue;
    }

    @Override
    public void run() {

        int rejected = 0;
        List<Future<FeatureVectorFactory.FeatureVector>> futures = new ArrayList<>();
        while(this.loader.hasNext()) {
            BioFormatsImage image = loader.next();
            try {
                futures.add(executor.submit(() -> factory.computeVector(image)));
                count++;
            } catch (RejectedExecutionException e) {
                rejected++;
                log.error("Rejected task " + rejected);
            }
        }
        log.error("Encountered " + rejected + " rejected tasks");

        for (Future<FeatureVectorFactory.FeatureVector> f : futures) {
            try {
                resultsQueue.add(f.get());
            } catch (InterruptedException e) {
                log.error("Interrupted exception when waiting for future");
                e.printStackTrace();
            } catch (ExecutionException e) {
                log.error("Execution exception in future");
                e.printStackTrace();
            }
        }
    }
}
