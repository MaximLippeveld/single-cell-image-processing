package be.maximl.bf.lib;

import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.KnownConstant;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.mask.integer.DefaultMaskInterval;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Predicate;

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

  private static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Integer> channels;
  private short[] planes;
  private ArrayImg<UnsignedShortType, ShortArray> img;
  private ArrayImg<NativeBoolType, BooleanArray> maskImg;
  private boolean[] masks;
  private long[] planeLengths;
  private long[] dims = null;

  public short[] getPlanes() {
    return planes;
  }

  public void setPlanes(short[] planes) {
    this.planes = planes;
    img =  ArrayImgs.unsignedShorts(planes, getDims());
  }

  public long[] getPlaneLengths() {
    return planeLengths;
  }

  public void setMasks(boolean[] masks) {
    this.masks = masks;
    maskImg = ArrayImgs.booleans(masks, getDims());
  }

  public void setPlaneLengths(long[] planeLengths) {
    this.planeLengths = planeLengths;
  }

  private long[] getDims() {
    if (this.dims == null) {
      this.dims = new long[3];
      this.dims[0] = this.planeLengths[0];
      this.dims[1] = this.planeLengths[1];
      this.dims[2] = channels.size();
    }
    return this.dims;
  }

  public RandomAccessibleInterval<UnsignedShortType> getImg() {
    return img;
  }

  public ArrayImg<NativeBoolType, BooleanArray> getMaskImg() {
    return maskImg;
  }

  public MaskInterval getMask(int pos) {
    RandomAccess<NativeBoolType> mask = Views.hyperSlice(getMaskImg(), CHANNELDIM, pos).randomAccess();
    Predicate<Localizable> maskPredicate = loc -> {
      mask.setPosition(loc);
      return mask.get().get();
    };
    return new DefaultMaskInterval(Views.hyperSlice(getImg(), CHANNELDIM, pos), BoundaryType.CLOSED, maskPredicate, KnownConstant.UNKNOWN);
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
