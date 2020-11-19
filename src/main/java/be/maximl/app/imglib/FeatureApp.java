package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import be.maximl.bf.lib.RecursiveExtensionFilteredLister;
import io.scif.FormatException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Ingest metadata from a directory containing photos, make them available
 * as EXIF.
 * 
 * @author jgp
 */
public class FeatureApp {
  public static void main(String[] args) {
    FeatureApp app = new FeatureApp();
    try {
      app.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    lister.setFileLimit(2);
    lister.setPath(importDirectory);
    lister.addExtension("cif");

    BioFormatsLoader relation = new BioFormatsLoader();
    relation.addChannel(0);
    relation.setImageLimit(10);
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
    BlockingQueue<BioFormatsImage> queue = new ArrayBlockingQueue<>(queueCapacity);
    BlockingQueue<FeatureVectorFactory.FeatureVector> writeQueue = new LinkedBlockingQueue<>();

    ImageProducer producer = new ImageProducer(relation, queue);
    producer.start();

    ImageConsumer consumer = new ImageConsumer(queue, writeQueue);
    consumer.start();

    File file = new File("output.csv");
    CsvWriter writer = new CsvWriter(writeQueue, file);
    writer.start();

    try {
      producer.join();
      consumer.interrupt();

      while(!writeQueue.isEmpty()) {
        writeQueue.wait();
      }
      writer.interrupt();
      writer.join();
    } catch (InterruptedException e) {
      System.err.println("Main thread interrupted while joining");
    } finally {
      consumer.interrupt();
      producer.interrupt();
      writer.interrupt();
    }

    final long endTime = System.currentTimeMillis();
    double execTime = (endTime - startTime)/1000.;
    System.out.println("Execution time in s: " + execTime);

    return true;
  }
}
