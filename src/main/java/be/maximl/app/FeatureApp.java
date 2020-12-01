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
import org.apache.commons.cli.*;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Plugin(type = Command.class, menuPath = "Plugins>SCI Feature extraction")
public class FeatureApp<T extends RealType<T>> implements Command {

  private static final String IMAGELIMIT_DESC = "Maximum number of images to load (-1 loads all images).";
  private static final String INPUTDIR_DESC = "Directory containing input files.";
  private static final String FILELIMIT_DESC = "Maximum number of files to process (-1 processes all files).";
  private static final String OUTPUTDIR_DESC = "Directory to which output may be written.";
  private static final String OUTPUTFILENAME_DESC = "Filename of file containing feature vectors.";
  private static final String CHANNELS_DESC = "Channels to process (comma-separated).";
  private static final String EXTENSIONS_DESC = "Extensions to scan for (comma-separated).";
  private static final String POOLSIZE_DESC = "Specify the amount of executors used for feature computation. Default is number of processors.";

  @Parameter
  private OpService opService;

  @Parameter
  private StatusService statusService;

  @Parameter
  private LogService log;

  @Parameter(label="Image limit", description = FeatureApp.IMAGELIMIT_DESC)
  private int imageLimit;

  @Parameter(label="File limit", description = FeatureApp.FILELIMIT_DESC)
  private int fileLimit;

  @Parameter(label="Channels", description=FeatureApp.CHANNELS_DESC)
  private String channels;

  @Parameter(label="Extensions", description = FeatureApp.EXTENSIONS_DESC)
  private String extensions;

  @Parameter(label="Input directory", description = FeatureApp.INPUTDIR_DESC)
  private File inputDirectory;

  @Parameter(label="Output directory", description = FeatureApp.OUTPUTDIR_DESC)
  private File outputDirectory;

  @Parameter(label="Output filename", description = FeatureApp.OUTPUTFILENAME_DESC)
  private String outputFilename;

  @Parameter(label="Executor pool size", description = FeatureApp.POOLSIZE_DESC)
  private int executorPoolSize;

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
    ExecutorService executor = new ThreadPoolExecutor(executorPoolSize, executorPoolSize, 1000, TimeUnit.MILLISECONDS, queue);
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


    final long startTime = System.currentTimeMillis();
    producer.start();

    try {
      File output = new File(outputDirectory, outputFilename);
      CsvWriter csvWriter = new CsvWriter(log, completionService, output, statusService);
      csvWriter.start();

      try {
        producer.join();
        int producerCount = counter.get();
        log.info("PRODUCER COUNT " + producerCount);

        long endTime = System.currentTimeMillis();
        double execTime = (endTime - startTime)/1000.;
        log.info("Task producer finished after " + execTime + "s");

        executor.shutdown();
        boolean res = executor.awaitTermination(1, TimeUnit.SECONDS);
        log.info("Task executor finished " + res);

        endTime = System.currentTimeMillis();
        execTime = (endTime - startTime)/1000.;
        log.info("Task executor finished after " + execTime + "s");

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
        endTime = System.currentTimeMillis();
        execTime = (endTime - startTime)/1000.;
        log.info("CSV Writer finished after " + execTime + "s");
      } catch (InterruptedException e) {
        log.error("Interrupted while shutting down");
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * Starts the application when called from command line
   */
  public static void main(String[] args) throws ParseException {

    Options options = new Options();
    options.addOption("o", "outputDirectory", true, FeatureApp.OUTPUTDIR_DESC);
    options.addOption("f", "outputFilename", true, FeatureApp.OUTPUTFILENAME_DESC);
    options.addOption("il", "imageLimit", true, FeatureApp.IMAGELIMIT_DESC);
    options.addOption("fl", "fileLimit", true, FeatureApp.FILELIMIT_DESC);
    options.addOption("h", "help", false, "Print usage.");
    options.addOption("ex", "executorPoolSize", true, FeatureApp.POOLSIZE_DESC);
    options.addRequiredOption("i", "inputDirectory", true, FeatureApp.INPUTDIR_DESC);
    options.addRequiredOption("c", "channels", true, FeatureApp.CHANNELS_DESC);
    options.addRequiredOption("e", "extensions", true, FeatureApp.EXTENSIONS_DESC);

    HelpFormatter formatter = new HelpFormatter();

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse( options, args);

      if(cmd.hasOption("help")) {
        if ((boolean)cmd.getParsedOptionValue("help")) {
          formatter.printHelp("SCI Feature extraction tool", options);
        }
      }

      final Map<String, Object> inputArgs = new HashMap<>();
      inputArgs.put("imageLimit", -1);
      inputArgs.put("fileLimit", -1);
      inputArgs.put("outputFilename", "output_allstains1.csv");
      inputArgs.put("outputDirectory", new File("."));
      inputArgs.put("executorPoolSize", Runtime.getRuntime().availableProcessors());

      for (Option option : cmd.getOptions()) {
        String longOpt = option.getLongOpt();
        if (longOpt.matches("^.+Directory$")) {
          inputArgs.put(longOpt, new File(option.getValue()));
        } else {
          inputArgs.put(longOpt, option.getValue());
        }
      }

      final ImageJ ij = new ImageJ();

      Future<CommandModule> command = ij.command().run(FeatureApp.class, true, inputArgs);
      ij.module().waitFor(command);
      ij.getContext().dispose();
    } catch (MissingOptionException e) {
      formatter.printHelp( "SCI Feature extraction tool", options);
    }
  }
}
