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

    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private ICSVWriter csvWriter;
    final private Writer writer;

    public CsvWriter(LogService log, CompletionService<FeatureVectorFactory.FeatureVector> completionService, File file, StatusService statusService) throws IOException {
        super(log, statusService);
        this.completionService = completionService;

        writer = new FileWriter(file);
        CSVWriterBuilder builder = new CSVWriterBuilder(writer);
        builder.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER);
        csvWriter = builder.build();
    }

    @Override
    public void run() {

        try {
            try {
                Future<FeatureVectorFactory.FeatureVector> vec;
                while (!Thread.currentThread().isInterrupted()) {
                    vec = completionService.take();

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
