package be.maximl.bf.lib;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * A good old JavaBean containing the EXIF properties as well as the
 * SparkColumn annotation.
 * 
 * @author jgp
 */
public class BioFormatsImage implements Serializable {
  private static transient Logger log = LoggerFactory.getLogger(
      BioFormatsImage.class);
  private static final long serialVersionUID = -2589804417011601051L;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Integer> channels;
  private short[] planes;
  private long[] planeLengths;
  private long[] dims = null;

  public short[] getPlanes() {
    return planes;
  }

  public void setPlanes(short[] planes) {
    this.planes = planes;
  }

  public long[] getPlaneLengths() {
    return planeLengths;
  }

  public void setPlaneLengths(long[] planeLengths) {
    this.planeLengths = planeLengths;
  }

  private long[] getDims() {
    if (this.dims == null) {
      this.dims = new long[3];
      this.dims[0] = this.channels.size();
      this.dims[1] = this.planeLengths[0];
      this.dims[2] = this.planeLengths[1];
    }
    return this.dims;
  }

  public ArrayImg<UnsignedShortType, ShortArray> getImg() {
    return ArrayImgs.unsignedShorts(this.planes, this.getDims());
  }

  /**
   * @return the directory
   */
  public String getDirectory() {
    return directory;
  }

  /**
   * @return the extension
   */
  public String getExtension() {
    return extension;
  }


  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return the size
   */
  public long getSize() {
    return size;
  }

  public List<Integer> getChannels() {
    return channels;
  }

  public void setChannels(List<Integer> channels) {
    this.channels = channels;
  }


  /**
   * @param directory
   *          the directory to set
   */
  public void setDirectory(String directory) {
    this.directory = directory;
  }

  /**
   * @param extension
   *          the extension to set
   */
  public void setExtension(String extension) {
    this.extension = extension;
  }


  /**
   * @param filename
   *          the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }


  /**
   * @param size
   *          the size to set
   */
  public void setSize(long size) {
    this.size = size;
  }

}
