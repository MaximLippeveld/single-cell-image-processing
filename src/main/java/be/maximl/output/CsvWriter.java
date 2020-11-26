package be.maximl.output;

import be.maximl.feature.FeatureVectorFactory;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.scijava.log.LogService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvWriter extends Thread {

    final private LogService log;
    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private ICSVWriter csvWriter;
    final private Writer writer;
    final private AtomicInteger countWritten = new AtomicInteger();

    public CsvWriter(LogService log, CompletionService<FeatureVectorFactory.FeatureVector> completionService, File file) throws IOException {
        this.log = log;
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

                    if (countWritten.get() == 0) {
                        String[] headers = vec.get().getMap().keySet().toArray(new String[0]);
                        csvWriter.writeNext(headers);
                    }

                    csvWriter.writeNext(vec.get().getLine());

                    synchronized (countWritten) {
                        countWritten.getAndIncrement();
                        countWritten.notify();
                    }
                }

            } catch (ExecutionException | InterruptedException e) {
                log.error("Interrupted while waiting");
                e.printStackTrace();
            } finally {
                log.info("Finalize writer");
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AtomicInteger getCountWritten() {
        return countWritten;
    }
}
