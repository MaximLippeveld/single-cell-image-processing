package be.maximl.data.bf;

import be.maximl.data.Image;
import ij.process.ImageProcessor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.List;

/**
 * A good old JavaBean containing the EXIF properties as well as the
 * SparkColumn annotation.
 * 
 * @author jgp
 */
public class BioFormatsImage<T extends RealType<T>, S extends BooleanType<S>> implements Image<T, S> {
  private static final long serialVersionUID = -2589804417011601051L;

  final private static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Integer> channels;
  private Img<T> img;
  private Img<S> maskImg;
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
  public void setPlanes(Img<T> planes) {
    img = planes;
  }

  @Override
  public void setMasks(Img<S> masks) {
    maskImg = masks;
  }

  @Override
  public void setPlaneLengths(long[] planeLengths) {
    dims[0] = planeLengths[0];
    dims[1] = planeLengths[1];
  }

  @Override
  public long[] getDimensions() {
    return dims;
  }

  @Override
  public RandomAccessibleInterval<T> getRAI() {
    return img;
  }

  private Img<S> getMaskImg() {
    return maskImg;
  }

  @Override
  public ImgFactory<T> getFactory() {
    return img.factory();
  }

  @Override
  public MaskInterval getMaskInterval(int pos) {
    return Masks.toMaskInterval(Views.hyperSlice(getMaskImg(), 2, 0));
  }

  @Override
  public IterableInterval<S> getMaskAsIterableInterval(int pos) {
    return Views.hyperSlice(getMaskImg(), 2, pos);
  }

  @Override
  public ImageProcessor getMaskAsImageProcessor(int i) {
    return ImageJFunctions.wrap(getMaskImg(), "test").getProcessor();
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
