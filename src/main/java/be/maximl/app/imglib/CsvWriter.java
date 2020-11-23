package be.maximl.app.imglib;

import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvWriter extends Thread {

    final private Logger log = LoggerFactory.getLogger(CsvWriter.class);
    final private CompletionService<FeatureVectorFactory.FeatureVector> completionService;
    final private ICSVWriter csvWriter;
    final private Writer writer;
    final private AtomicInteger countWritten = new AtomicInteger();
    volatile public boolean stopWriting;

    public CsvWriter(CompletionService<FeatureVectorFactory.FeatureVector> completionService, File file) throws IOException {
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
