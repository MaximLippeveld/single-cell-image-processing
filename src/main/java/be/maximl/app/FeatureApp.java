package be.maximl.app;

import be.maximl.data.FileLister;
import be.maximl.data.Image;
import be.maximl.data.Loader;
import be.maximl.data.bf.BioFormatsLoader;
import be.maximl.data.RecursiveExtensionFilteredLister;
import be.maximl.feature.FeatureVectorFactory;
import be.maximl.output.CsvWriter;
import be.maximl.output.FeatureVecWriter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.scif.FormatException;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.apache.commons.cli.*;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


@Plugin(type = Command.class, menuPath = "Plugins>SCI Feature extraction")
public class FeatureApp implements Command {

  private static final String IMAGELIMIT_DESC = "Maximum number of images to load (-1 loads all images).";
  private static final String INPUTDIR_DESC = "Directory containing input files.";
  private static final String FILELIMIT_DESC = "Maximum number of files to process (-1 processes all files).";
  private static final String OUTPUTDIR_DESC = "Directory to which output may be written.";
  private static final String OUTPUTFILENAME_DESC = "Filename of file containing feature vectors.";
  private static final String CHANNELS_DESC = "Channels to process (comma-separated).";
  private static final String EXTENSIONS_DESC = "Extensions to scan for (comma-separated).";
  private static final String POOLSIZE_DESC = "Specify the amount of executors used for feature computation. Default is number of processors.";
  private static final String FEATURESET_DESC = "Specify which featureset to compute.";
  private static final String YAMLCONFIG_DESC = ".yml config file containing input files and features to compute.";

  static class Config {
    private List<File> files;
    private List<String> features;

    public List<File> getFiles() {
      return files;
    }

    public List<String> getFeatures() {
      return features;
    }

    public void setFiles(List<File> files) {
      this.files = files;
    }

    public void setFeatures(List<String> features) {
      this.features = features;
    }
  }

  @Parameter
  private OpService opService;

  @Parameter
  private StatusService statusService;

  @Parameter
  private LogService log;

  @Parameter(label="Channels", description=FeatureApp.CHANNELS_DESC, persist = false)
  private String channels;

  @Parameter(label="Input directory", description = FeatureApp.INPUTDIR_DESC, required = false, persist = false)
  private File inputDirectory = null;

  @Parameter(label="Extensions", description = FeatureApp.EXTENSIONS_DESC, required = false, persist = false)
  private String extensions = null;

  @Parameter(label="Image limit", description = FeatureApp.IMAGELIMIT_DESC, required = false, persist = false)
  private int imageLimit = -1;

  @Parameter(label="File limit", description = FeatureApp.FILELIMIT_DESC, required = false, persist = false)
  private int fileLimit = -1;

  @Parameter(label="Output directory", description = FeatureApp.OUTPUTDIR_DESC, required = false, persist = false)
  private File outputDirectory = new File(".");

  @Parameter(label="Output filename", description = FeatureApp.OUTPUTFILENAME_DESC, required = false, persist = false)
  private String outputFilename = "output.csv";

  @Parameter(label="Executor pool size", description = FeatureApp.POOLSIZE_DESC, required = false, persist = false)
  private int executorPoolSize= Runtime.getRuntime().availableProcessors();

  @Parameter(label="Feature set", description = FeatureApp.FEATURESET_DESC, required = false, persist = false)
  private String featureSet = null;

  @Parameter(label="YAML config", description = FeatureApp.YAMLCONFIG_DESC, required = false, persist = false)
  private File yamlConfig = null;

  @Override
  public void run() {

    FileLister lister;
    List<String> features;
    if (yamlConfig != null) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        Config config = mapper.readValue(yamlConfig, Config.class);

        //yaml present
        lister = (FileLister) () -> config.files;
        features = config.features;
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    } else {
      RecursiveExtensionFilteredLister refLister = new RecursiveExtensionFilteredLister();
      refLister.setFileLimit(fileLimit);
      refLister.setPath(inputDirectory.getPath());
      for (String extension : extensions.split(",")) {
        refLister.addExtension(extension);
      }

      lister = refLister;
      features = Arrays.asList("size", "sizeConvexHull", "sizeMask");
    }

    Loader<UnsignedShortType, NativeBoolType> loader = new BioFormatsLoader(log);
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

    FeatureVectorFactory<UnsignedShortType, NativeBoolType> factory = new FeatureVectorFactory<>(opService, features);

    Thread producer = new Thread(() -> {
      int rejected = 0;
      while(loader.hasNext()) {
        Image<UnsignedShortType, NativeBoolType> image = loader.next();
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
      FeatureVecWriter writer = new CsvWriter(log, completionService, output, statusService);
      writer.start();

      try {
        producer.join();
        int producerCount = counter.get();
        log.info("PRODUCER COUNT " + producerCount);

        long endTime = System.currentTimeMillis();
        double execTime = (endTime - startTime)/1000.;
        log.info("Task producer finished after " + execTime + "s");

        if (writer.isAlive()) {
          synchronized (writer.getHandled()) {
            while(producerCount != writer.getHandled().get()) {
              writer.getHandled().wait();
            }
          }
          writer.interrupt();
          writer.join();
        }
        log.info("WRITER COUNT " + writer.getHandled());
        endTime = System.currentTimeMillis();
        execTime = (endTime - startTime)/1000.;
        log.info("CSV Writer finished after " + execTime + "s");

        executor.shutdown();
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
    options.addOption("fs", "featureSet", true, FeatureApp.FEATURESET_DESC);
    options.addOption("e", "extensions", true, FeatureApp.EXTENSIONS_DESC);
    options.addOption("y", "yamlConfig", true, FeatureApp.YAMLCONFIG_DESC);
    options.addOption("i", "inputDirectory", true, FeatureApp.INPUTDIR_DESC);
    options.addRequiredOption("c", "channels", true, FeatureApp.CHANNELS_DESC);

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

      for (Option option : cmd.getOptions()) {
        String longOpt = option.getLongOpt();
        if (longOpt.matches("^.+Directory$") | longOpt.matches("yamlConfig")) {
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
      e.printStackTrace();
      formatter.printHelp( "SCI Feature extraction tool", options);
    }
  }
}
