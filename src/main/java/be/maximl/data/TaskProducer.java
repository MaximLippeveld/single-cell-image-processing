package be.maximl.data;

import be.maximl.feature.FeatureVectorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Iterator;
import java.util.concurrent.CompletionService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProducer<T extends NativeType<T> & RealType<T>> extends Thread {

    final private Iterator<Image<T>> iterator;
    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private FeatureVectorFactory<T> factory;
    final private AtomicInteger counter;

    public TaskProducer(Iterator<Image<T>> iterator, CompletionService<FeatureVectorFactory.FeatureVector> completionService, FeatureVectorFactory<T> factory, AtomicInteger counter) {
        this.iterator = iterator;
        this.completionService = completionService;
        this.factory = factory;
        this.counter = counter;
    }

    @Override
    public void run() {
        boolean submitted;
        while(iterator.hasNext()) {
            Image<T> image = iterator.next();

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
