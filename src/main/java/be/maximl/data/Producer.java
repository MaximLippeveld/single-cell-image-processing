package be.maximl.data;

import be.maximl.feature.FeatureVectorFactory;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.concurrent.CompletionService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer <T extends NativeType<T> & RealType<T>> extends Thread {

    final private Loader<T> loader;
    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private FeatureVectorFactory<T> factory;
    final private AtomicInteger counter;

    public Producer(Loader<T> loader, CompletionService<FeatureVectorFactory.FeatureVector> completionService, FeatureVectorFactory<T> factory, AtomicInteger counter) {
        this.loader = loader;
        this.completionService = completionService;
        this.factory = factory;
        this.counter = counter;
    }

    @Override
    public void run() {
        boolean submitted;
        while(loader.hasNext()) {
            Image<T> image = loader.next();

            if (image != null) {
                submitted = false;

                while(!submitted) {
                    try {
                        completionService.submit(() -> factory.computeVector(image));
                        counter.incrementAndGet();
                        submitted = true;
                    } catch (RejectedExecutionException e) {
                        try {
                            synchronized (completionService) {
                                completionService.wait();
                            }
                        } catch (InterruptedException ignored) { }
                    }
                }
            }
        }
    }
}
