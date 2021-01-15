/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 - 2021 Maxim Lippeveld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package be.maximl.data;

import be.maximl.data.loaders.Loader;
import be.maximl.feature.FeatureVectorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.concurrent.CompletionService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProducer<T extends NativeType<T> & RealType<T>> extends Thread {

    final private Loader<T> loader;
    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private FeatureVectorFactory<T> factory;
    final private AtomicInteger counter;

    public TaskProducer(Loader<T> loader, CompletionService<FeatureVectorFactory.FeatureVector> completionService, FeatureVectorFactory<T> factory, AtomicInteger counter) {
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
                        completionService.submit(() -> factory.computeVector(image, loader.isMasked()));
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
