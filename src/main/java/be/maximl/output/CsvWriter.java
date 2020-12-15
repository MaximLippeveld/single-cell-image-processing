/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 Maxim Lippeveld
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
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.*;

public class CsvWriter extends FeatureVecWriter {

    private ICSVWriter csvWriter;
    private Writer writer;

    public CsvWriter(LogService log, StatusService statusService, CompletionService<FeatureVectorFactory.FeatureVector> completionService, String file) {
        super(log, statusService, completionService);

        csvWriter = null;
        writer = null;

        try {
            writer = new FileWriter(file);
            CSVWriterBuilder builder = new CSVWriterBuilder(writer);
            builder.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER);
            csvWriter = builder.build();
        } catch (IOException e) {
            log.error(e);
            System.exit(1);
        }
    }

    @Override
    public void run() {

        try {
            try {
                Future<FeatureVectorFactory.FeatureVector> vec;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (completionService) {
                        vec = completionService.take();
                        completionService.notify();
                    }

                    if (handleCount.get() == 0) {
                        String[] headers = vec.get().getMap().keySet().toArray(new String[0]);
                        csvWriter.writeNext(headers);
                    }

                    csvWriter.writeNext(vec.get().getLine());

                    synchronized (handleCount) {
                        int c = handleCount.getAndIncrement();

                        if (c % 1000 == 0) {
                            log.info("Written " + c + " vectors.");
                        }

                        handleCount.notify();
                    }
                }

            } catch (ExecutionException e) {
                log.error("Exception while computing a feature vector.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting");
            } finally {
                log.info("Finalize writer");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
