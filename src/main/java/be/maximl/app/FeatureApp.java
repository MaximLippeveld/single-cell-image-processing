/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 Maxim Lippeveld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package be.maximl.app;

import be.maximl.data.*;
import be.maximl.data.loaders.Loader;
import be.maximl.data.loaders.imp.CIFLoader;
import be.maximl.data.loaders.imp.TIFFLoader;
import be.maximl.data.validators.ConnectedComponentsValidator;
import be.maximl.data.validators.Validator;
import be.maximl.feature.FeatureVectorFactory;
import be.maximl.output.CsvWriter;
import be.maximl.output.FeatureVecWriter;
import be.maximl.output.SQLiteWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.scif.SCIFIO;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
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
import java.util.stream.Collectors;


/**
 * Computes descriptive image features for images contained in specified files. Can be used as an
 * ImageJ plugin or as a standalone package through a CLI interface.
 */
@Plugin(type = Command.class, menuPath = "Plugins>SCI Feature extraction")
public class FeatureApp<T extends NativeType<T> & RealType<T>> implements Command {

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

  /**
   * Contains configuration loaded from a YAML-file.
   */
  static class Config {
    private List<File> files;
    private List<String> features = Collections.emptyList();
    private String loader;

    public String getLoader() {
      return loader;
    }

    public void setLoader(String loader) {
      this.loader = loader;
    }

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

  @Parameter
  private SCIFIO scifio;

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

  /**
   * Orchestrates the entire feature computation process from reading in parameters/configuration to
   * handling the lifecycle of all processing threads.
   *
   * The method proceeds as follows:
   * <ol>
   *     <li>setting up a <a href="#{@link}>{@link FileLister} to iterate over the input files,</a></li>
   *     <li>determining which features to compute and setting up a <a href="#{@link}">{@link FeatureVectorFactory}</a>,</li>
   *     <li>setting up a <a href="#{@link}>{@link Loader} to iterate over all images,</a></li>
   *     <li>starting threads for submitting tasks to the factory and writing the output, and
   *     instantiating a <a href="#{@link}">{@link CompletionService}</a> for handling the feature computation tasks,</li>
   *     <li>and, finally, handling the teardown of all threads.</li>
   * </ol>
   */
  @Override
  public void run() {

    /*
    * Input files and the featureset to compute are either passed
    * on through the @Parameter-annotated class members or in a YAML-file.
    */
    FileLister lister;
    List<String> features;
    String loaderType = "";
    if (yamlConfig != null) {
      // read config from yaml
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      try {
        Config config = mapper.readValue(yamlConfig, Config.class);

        lister = (FileLister) () -> config.files;
        log.info(config.files);
        features = config.features;
        loaderType = config.loader;
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }
    } else {
      // read config from class members
      RecursiveExtensionFilteredLister refLister = new RecursiveExtensionFilteredLister();
      refLister.setFileLimit(fileLimit);
      refLister.setPath(inputDirectory.getPath());
      for (String extension : extensions.split(",")) {
        refLister.addExtension(extension);
      }

      lister = refLister;
      features = Collections.emptyList();

      if (extensions.contains("cif")) {
        loaderType = "cif";
      } else if (extensions.contains("tif") | extensions.contains("tiff")) {
        loaderType = "tif";
      } else {
        log.error("Unknown loader type");
        return;
      }
    }

    boolean computeAllFeatures = features.size() == 0;
    int queueCapacity = 50000;
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
    ExecutorService executor = new ThreadPoolExecutor(executorPoolSize, executorPoolSize, 1000, TimeUnit.MILLISECONDS, queue);
    CompletionService<FeatureVectorFactory.FeatureVector> completionService = new ExecutorCompletionService<>(executor);
    AtomicInteger counter = new AtomicInteger(0);
    List<Long> longChannels = Arrays.stream(channels.split(",")).map(Long::parseLong).collect(Collectors.toList());

    FeatureVectorFactory<T> factory = new FeatureVectorFactory<>(opService, log, features, computeAllFeatures);
    Validator<T> validator = new ConnectedComponentsValidator<>(opService);

    Loader<T> loader;
    switch (loaderType) {
      case "tif":
        loader = new TIFFLoader<>(lister.getFiles().iterator(), longChannels, log, scifio);
        break;
      default:
        loader = new CIFLoader<>(log, imageLimit, longChannels, lister.getFiles().iterator(), scifio, validator);
        break;
    }
    TaskProducer<T> taskProducer = new TaskProducer<>(loader, completionService, factory, counter);

    final long startTime = System.currentTimeMillis();
    taskProducer.start();

    File output = new File(outputDirectory, outputFilename);
    FeatureVecWriter writer;
    switch (FilenameUtils.getExtension(outputFilename)) {
      case "sqlite3":
        writer = new SQLiteWriter(log, statusService, completionService, output.getPath());
        break;
      case "csv":
        writer = new CsvWriter(log, statusService, completionService, output.getPath());
        break;
      default:
        log.error("Output extension isn't recognized");
        return;
    }
    writer.start();

    try {
      taskProducer.join(); // wait for the producer to put all images on the task queue
      int producerCount = counter.get();
      log.info("PRODUCER COUNT " + producerCount);
      log.info("Validator flagged " + validator.getInvalidCount() + " images");

      long endTime = System.currentTimeMillis();
      double execTime = (endTime - startTime)/1000.;
      log.info("Task producer finished after " + execTime + "s");

      if (writer.isAlive()) {
        /*
         * Ask the writer continuously how many vectors it has written.
         * Once it has handled as many vectors as the producer has produced tasks
         * the writer is interrupted.
         */
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
