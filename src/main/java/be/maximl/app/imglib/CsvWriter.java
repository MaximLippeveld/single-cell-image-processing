package be.maximl.app.imglib;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CsvWriter extends Thread {

    final private Logger log = LoggerFactory.getLogger(CsvWriter.class);
    final private BlockingQueue<FeatureVectorFactory.FeatureVector> queue;
    final private StatefulBeanToCsv<Object> beanToCsv;
    final private Writer writer;
    public int count = 0;
    volatile public boolean stopWriting = false;

    public CsvWriter(BlockingQueue<FeatureVectorFactory.FeatureVector> queue, File file) throws IOException {
        this.queue = queue;

        writer = new FileWriter(file);
        beanToCsv = new StatefulBeanToCsvBuilder<>(writer).build();
    }

    @Override
    public void run() {

        try {
            try {
                FeatureVectorFactory.FeatureVector vec;
                while (!stopWriting) {
                    vec = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (vec != null) {
                        beanToCsv.write(vec);
                        count++;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

                log.info("Writing remaining vectors to csv " + queue.size());
                for (FeatureVectorFactory.FeatureVector vec2 : queue) {
                    beanToCsv.write(vec2);
                    count++;
                }

                log.info("Finalize writer");
                writer.flush();
                writer.close();
            }
        } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
            e.printStackTrace();
        }
    }
}
