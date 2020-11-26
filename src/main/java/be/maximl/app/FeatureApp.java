package be.maximl.app;

import be.maximl.data.Image;
import be.maximl.data.Loader;
import be.maximl.data.bf.BioFormatsLoader;
import be.maximl.data.RecursiveExtensionFilteredLister;
import be.maximl.feature.FeatureVectorFactory;
import be.maximl.output.CsvWriter;
import io.scif.FormatException;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Plugin(type = Command.class, menuPath = "Plugins>SCI Feature extraction")
public class FeatureApp<T extends RealType<T>> implements Command {

  @Parameter
  private OpService opService;

  @Parameter
  private LogService log;

  @Parameter
  private int imageLimit;

  @Parameter
  private int fileLimit;

  @Parameter
  private String channels;

  @Parameter
  private String extensions;

  @Parameter
  private File inputDirectory;

  @Parameter
  private File outputFile;

  @Override
  public void run() {

    RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
    lister.setFileLimit(fileLimit);
    lister.setPath(inputDirectory.getPath());
    for(String extension : extensions.split(",")) {
      lister.addExtension(extension);
    }

    Loader loader = new BioFormatsLoader(log);
    for (String channel : channels.split(",")) {
      loader.addChannel(Integer.parseInt(channel));
    }

    loader.setImageLimit(imageLimit);
    try {
      loader.setLister(lister);
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

    FeatureVectorFactory factory = new FeatureVectorFactory(opService);
    Thread producer = new Thread(() -> {
      int rejected = 0;
      while(loader.hasNext()) {
        Image image = loader.next();
        try {
          completionService.submit(() -> factory.computeVector(image));
          counter.incrementAndGet();
        } catch (RejectedExecutionException e) {
          rejected++;
          log.error("Rejected task " + rejected);
        }
      }
      log.error("Encountered " + rejected + " rejected tasks");
    });
    producer.start();

    try {
      CsvWriter csvWriter = new CsvWriter(log, completionService, outputFile);
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * Starts the application when called from command line
   */
  public static void main(String[] args) {

    final Map<String, Object> inputArgs = new HashMap<>();
    inputArgs.put("inputDirectory", new File("/home/maximl/Data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/"));
    inputArgs.put("outputFile", new File("output.csv"));
    inputArgs.put("imageLimit", 1000);
    inputArgs.put("fileLimit", -1);
    inputArgs.put("channels", "0,3,5,6,10");
    inputArgs.put("extensions", "cif");

    final ImageJ ij = new ImageJ();

    // Import directory

    final long startTime = System.currentTimeMillis();

    Future<CommandModule> command = ij.command().run(FeatureApp.class, true, inputArgs);
    ij.module().waitFor(command);

    final long endTime = System.currentTimeMillis();
    double execTime = (endTime - startTime)/1000.;
    System.out.println("Execution time in s: " + execTime);

    ij.getContext().dispose();
  }
}
