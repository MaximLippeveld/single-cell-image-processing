package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import be.maximl.bf.lib.RecursiveExtensionFilteredLister;
import io.scif.FormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

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
    lister.setFileLimit(6);
    lister.setPath(importDirectory);
    lister.addExtension("cif");

    BioFormatsLoader relation = new BioFormatsLoader();
    relation.addChannel(0);
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

    int queueCapacity = 10000;
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
    BlockingQueue<FeatureVectorFactory.FeatureVector> resultsQueue = new LinkedBlockingQueue<>();

    ExecutorService executor = new ThreadPoolExecutor(10, 20, 1000, TimeUnit.MILLISECONDS, queue);

    ImageProducer producer = new ImageProducer(relation, executor, resultsQueue);
    producer.start();

    File file = new File("output.csv");
    CsvWriter csvWriter = new CsvWriter(resultsQueue, file);
    csvWriter.start();

    try {
      producer.join();
      log.info("PRODUCER COUNT " + producer.count);

      executor.shutdown();
      boolean res = executor.awaitTermination(1, TimeUnit.SECONDS);
      log.info("Executor tasks finished " + res);

      csvWriter.stopWriting = true;
      csvWriter.join();
      log.info("WRITER COUNT " + csvWriter.count);
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
