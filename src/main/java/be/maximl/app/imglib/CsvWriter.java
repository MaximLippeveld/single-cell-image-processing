package be.maximl.app.imglib;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;

public class CsvWriter extends Thread {

    final private Logger log = LoggerFactory.getLogger(CsvWriter.class);
    final private BlockingQueue<FeatureVectorFactory.FeatureVector> queue;
    final private StatefulBeanToCsv<Object> beanToCsv;
    final private Writer writer;

    public CsvWriter(BlockingQueue<FeatureVectorFactory.FeatureVector> queue, File file) throws IOException {
        this.queue = queue;

        writer = new FileWriter(file);
        beanToCsv = new StatefulBeanToCsvBuilder<>(writer).build();
    }

    @Override
    public void run() {
        try {
            while(!this.isInterrupted()) {
                synchronized (queue) {
                    queue.wait();
                }
                if (this.isInterrupted()) {
                    break;
                }

                FeatureVectorFactory.FeatureVector vec = queue.poll();
                beanToCsv.write(vec);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (CsvRequiredFieldEmptyException e) {
            e.printStackTrace();
        } catch (CsvDataTypeMismatchException e) {
            e.printStackTrace();
        } finally {
            try {
                log.debug("Ran writer finally");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
