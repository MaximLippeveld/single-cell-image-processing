package be.maximl.data.bf;

import be.maximl.data.Image;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.util.List;

/**
 * A good old JavaBean containing the EXIF properties as well as the
 * SparkColumn annotation.
 * 
 * @author jgp
 */
public class BioFormatsImage implements Image<UnsignedShortType, NativeBoolType> {
  private static final long serialVersionUID = -2589804417011601051L;

  final private static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Integer> channels;
  private short[] planes;
  private ArrayImg<UnsignedShortType, ShortArray> img;
  private ArrayImg<NativeBoolType, BooleanArray> maskImg;
  private boolean[] masks;
  final private long[] dims = new long[3];
  final private int id;

  public BioFormatsImage(int id) {
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public short[] getPlanes() {
    return planes;
  }

  @Override
  public void setPlanes(short[] planes) {
    this.planes = planes;
    img =  ArrayImgs.unsignedShorts(planes, getDims());
  }

  @Override
  public void setMasks(boolean[] masks) {
    this.masks = masks;
    maskImg = ArrayImgs.booleans(masks, getDims());
  }

  @Override
  public void setPlaneLengths(long[] planeLengths) {
    dims[0] = planeLengths[0];
    dims[1] = planeLengths[1];
  }

  private long[] getDims() {
    return dims;
  }

  @Override
  public RandomAccessibleInterval<UnsignedShortType> getImg() {
    return img;
  }

  private ArrayImg<NativeBoolType, BooleanArray> getMaskImg() {
    return maskImg;
  }

  @Override
  public ImgFactory<UnsignedShortType> getFactory() {
    return new ArrayImgFactory<>(new UnsignedShortType());
  }

  @Override
  public MaskInterval getMaskInterval(int pos) {
    return Masks.toMaskInterval(Views.hyperSlice(getMaskImg(), 2, 0));
  }

  @Override
  public IterableInterval<NativeBoolType> getMaskAsIterableInterval(int pos) {
    return Views.hyperSlice(getMaskImg(), 2, pos);
  }

  /**
   * @return the directory
   */
  @Override
  public String getDirectory() {
    return directory;
  }

  /**
   * @return the extension
   */
  @Override
  public String getExtension() {
    return extension;
  }


  /**
   * @return the filename
   */
  @Override
  public String getFilename() {
    return filename;
  }

  /**
   * @return the size
   */
  @Override
  public long getSize() {
    return size;
  }

  @Override
  public List<Integer> getChannels() {
    return channels;
  }

  @Override
  public void setChannels(List<Integer> channels) {
    this.channels = channels;
    dims[2] = channels.size();
  }


  /**
   * @param directory
   *          the directory to set
   */
  @Override
  public void setDirectory(String directory) {
    this.directory = directory;
  }

  /**
   * @param extension
   *          the extension to set
   */
  @Override
  public void setExtension(String extension) {
    this.extension = extension;
  }


  /**
   * @param filename
   *          the filename to set
   */
  @Override
  public void setFilename(String filename) {
    this.filename = filename;
  }


  /**
   * @param size
   *          the size to set
   */
  @Override
  public void setSize(long size) {
    this.size = size;
  }

}
