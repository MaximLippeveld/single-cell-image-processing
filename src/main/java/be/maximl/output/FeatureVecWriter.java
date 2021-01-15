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
