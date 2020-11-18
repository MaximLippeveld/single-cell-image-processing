package be.maximl.app;

import be.maximl.bf.ds.BioFormatsRelation;
import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.RecursiveExtensionFilteredLister;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.ReduceFunction;
import org.apache.spark.sql.*;

import java.util.List;

/**
 * Ingest metadata from a directory containing photos, make them available
 * as EXIF.
 * 
 * @author jgp
 */
public class AnalysisApp {
  public static void main(String[] args) {
    AnalysisApp app = new AnalysisApp();
    app.start();
  }

  /**
   * Starts the application
   *
   * @return <code>true</code> if all is ok.
   */
  private boolean start() {
    // Get a session
    SparkSession spark = SparkSession.builder()
            .appName("BioFormats to Dataset")
            .config("spark.executor.cores", 10)
            .master("local").getOrCreate();

    // Import directory
    String importDirectory = "/home/maximl/Data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/";

    // read the data
    final long startTime = System.currentTimeMillis();

    Encoder<BioFormatsImage> bfiEncoder = Encoders.bean(BioFormatsImage.class);

    RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
    lister.setFileLimit(2);
    lister.setPath(importDirectory);
    lister.addExtension("cif");

    BioFormatsRelation relation = new BioFormatsRelation();
    relation.addChannel(0);
    relation.setImageLimit(-1);
    relation.setLister(lister);

    List<BioFormatsImage> images = relation.collectData();
    Dataset<BioFormatsImage> ds = spark.createDataset(images, bfiEncoder).cache();

    long count = ds.count();

    float avgMax = ds.map(
            (MapFunction<BioFormatsImage, Long>) i -> i.getImg().max(0), Encoders.LONG()
    ).reduce((ReduceFunction<Long>) Long::sum).floatValue() / count;

    System.out.println("AvgMax " + avgMax);

    final long endTime = System.currentTimeMillis();

    double execTime = (endTime - startTime)/1000.;
    System.out.println("Execution time in s: " + execTime);

    return true;
  }
}
