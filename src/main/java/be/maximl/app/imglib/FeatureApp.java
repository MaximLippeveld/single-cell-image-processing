package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsLoader;
import be.maximl.bf.lib.RecursiveExtensionFilteredLister;
import io.scif.FormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ingest metadata from a directory containing photos, make them available
 * as EXIF.
 * 
 * @author jgp
 */
public class FeatureApp {

  private final Logger log = LoggerFactory.getLogger(FeatureApp.class);

  public static void main(String[] args) {
    FeatureApp app = new FeatureApp();
    try {
      app.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
    LoggerFactory.getLogger(FeatureApp.class).info("End of app");
  }

  /**
   * Starts the application
   *
   * @return <code>true</code> if all is ok.
   */
  private boolean start() throws IOException {

    // Import directory
    String importDirectory = "/home/maximl/Data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/";

    // read the data
    final long startTime = System.currentTimeMillis();

    RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
    lister.setFileLimit(5);
    lister.setPath(importDirectory);
    lister.addExtension("cif");

    BioFormatsLoader relation = new BioFormatsLoader();
    relation.addChannel(0);
    relation.addChannel(2);
//    relation.addChannel(5);
    relation.setImageLimit(-1);
    try {
      relation.setLister(lister);
    } catch (IOException e) {
      System.err.println("Unknown file");
      e.printStackTrace();
    } catch (FormatException e) {
      System.err.println("Unknown format");
      e.printStackTrace();
    }

    int queueCapacity = 50000;
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
    ExecutorService executor = new ThreadPoolExecutor(30, 30, 1000, TimeUnit.MILLISECONDS, queue);
    CompletionService<FeatureVectorFactory.FeatureVector> completionService = new ExecutorCompletionService<>(executor);
    AtomicInteger counter = new AtomicInteger(0);

    ImageProducer producer = new ImageProducer(relation, completionService, counter);
    producer.start();

    File file = new File("output.csv");
    CsvWriter csvWriter = new CsvWriter(completionService, file);
    csvWriter.start();

    try {
      producer.join();
      int producerCount = counter.get();
      log.info("PRODUCER COUNT " + producerCount);

      executor.shutdown();
      boolean res = executor.awaitTermination(1, TimeUnit.SECONDS);
      log.info("Executor tasks finished " + res);

      if (csvWriter.isAlive()) {
        synchronized (csvWriter.getCountWritten()) {
          while(producerCount != csvWriter.getCountWritten().get()) {
            csvWriter.getCountWritten().wait();
          }
        }
        csvWriter.interrupt();
        csvWriter.join();
      }
      log.info("WRITER COUNT " + csvWriter.getCountWritten());
    } catch (InterruptedException e) {
      log.error("Interrupted here");
      e.printStackTrace();
    }

    final long endTime = System.currentTimeMillis();
    double execTime = (endTime - startTime)/1000.;
    System.out.println("Execution time in s: " + execTime);

    return true;
  }
}
