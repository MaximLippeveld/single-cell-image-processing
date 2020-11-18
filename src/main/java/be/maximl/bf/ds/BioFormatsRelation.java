package be.maximl.bf.ds;

import be.maximl.bf.lib.BFUtils;
import be.maximl.bf.lib.BioFormatsImage;

import be.maximl.bf.lib.RecursiveExtensionFilteredLister;
import io.scif.FormatException;
import io.scif.SCIFIO;
import io.scif.filters.ReaderFilter;
import org.apache.spark.sql.SQLContext;
import org.scijava.io.location.FileLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Build a relation to return the EXIF data of photos in a directory.
 * 
 * @author jgp
 */
public class BioFormatsRelation {
  private static final long serialVersionUID = 4598175080399877334L;
  private static transient Logger log =
      LoggerFactory.getLogger(BioFormatsRelation.class);
  private SQLContext sqlContext;
  private RecursiveExtensionFilteredLister lister;
  final private ArrayList<Integer> channels = new ArrayList<>();
  private int imageLimit = -1;

  public void addChannel(int channel) {
    this.channels.add(channel);
  }

  public void setImageLimit(int imageLimit) {
    this.imageLimit = imageLimit;
  }

  /**
   * Interface with the real world: the "plumbing" between Spark and
   * existing data, in our case the classes in charge of reading the
   * information from the photos.
   * 
   * The list of photos will be "mapped" and transformed into a Row.
   */
  public List<BioFormatsImage> collectData() {
    SCIFIO scifio = new SCIFIO();
    List<File> filesToProcess = this.lister.getFiles();
    List<BioFormatsImage> list = new ArrayList<>();

    for (File fileToProcess : filesToProcess) {
      try {
        FileLocation loc = new FileLocation(fileToProcess);
        ReaderFilter reader = scifio.initializer().initializeReader(loc);
        list.addAll(BFUtils.processFromFilename(reader, this.imageLimit, this.channels));
      } catch (IOException | FormatException e) {
        log.error("Error in data loading", e);
      }
    }
    return list;
  }

  public void setLister(
      RecursiveExtensionFilteredLister lister) {
    this.lister = lister;
  }
}
