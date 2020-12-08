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
