package be.maximl.data.bf;

import be.maximl.data.*;
import be.maximl.data.validators.Validator;
import io.scif.*;
import io.scif.config.SCIFIOConfig;
import io.scif.filters.ReaderFilter;
import io.scif.img.IO;
import io.scif.img.ImageRegion;
import io.scif.img.Range;
import io.scif.img.SCIFIOImgPlus;
import io.scif.img.cell.SCIFIOCellImgFactory;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class BioFormatsLoader<T extends NativeType<T> & RealType<T>> implements Loader<T> {
  private final LogService log;
  private Iterator<File> lister;
  final private List<Long> channels;
  final private SCIFIO scifio;
  final private Validator<T> validator;
  private int imageLimit = -1;

  private Reader currentReader;
  private int currentFinalIndex;
  private int currentIndex = 0;

  final private ImgFactory<T> imageFactory;
  final private ImgFactory<NativeBoolType> maskFactory;

  private Iterator<SCIFIOImgPlus<NativeBoolType>> maskIterator;
  private Iterator<SCIFIOImgPlus<T>> imageIterator;

  /**
   * Prevents SCIFIO's ImgOpener.openImgs method from closing the reader after reading in an image.
   * Closing the reader is handled by the us.
   */
  public static class CloseNoOpReader extends ReaderFilter {

    /**
     * @param r - Reader to be wrapped
     */
    public CloseNoOpReader(Reader r) {
      super(r);
    }

    @Override
    public void close() {
      return;
    }
  }

  public BioFormatsLoader(LogService log, List<Long> channels, SCIFIO scifio, Validator<T> validator, T imageType) {
    this.log = log;
    this.channels = channels;
    this.validator = validator;
    this.scifio = scifio;

    imageFactory = new ArrayImgFactory<>(imageType);
    maskFactory = new ArrayImgFactory<>(new NativeBoolType());
  }

  private Image<T> createImage(Reader reader, int id, Img<T> planes, Img<NativeBoolType> mask) {
    Image<T> image = new BioFormatsImage<>(id);
    image.setDirectory(reader.getMetadata().getSourceLocation().getURI().getPath());
    image.setFilename(reader.getMetadata().getSourceLocation().getName());
    image.setExtension(reader.getFormatName());
    image.setChannels(channels);
    image.setAxesLengths(planes.dimensionsAsLongArray());

    image.setMasks(mask);
    image.setPlanes(planes);

    return image;
  }

  private <U extends RealType<U>> Iterator<SCIFIOImgPlus<U>> getIterator(Iterator<Integer> indices, ImgFactory<U> factory) {

    SCIFIOConfig config = new SCIFIOConfig();
    config.imgOpenerSetOpenAllImages(false);
    config.imgOpenerSetRegion(
            new ImageRegion(new AxisType[]{Axes.CHANNEL}, new Range(channels.stream().mapToLong(l -> l).toArray())));

    CloseNoOpReader noOpreader = new CloseNoOpReader(currentReader);

    return new Iterator<SCIFIOImgPlus<U>>() {
      @Override
      public boolean hasNext() {
        return indices.hasNext();
      }

      @Override
      public SCIFIOImgPlus<U> next() {
        int imgIndex = indices.next();
        config.imgOpenerSetIndex(imgIndex);
        return IO.open(noOpreader, factory.type(), factory, config);
      }
    };
  }

  private void initializeNewReader() throws IOException, FormatException {
    currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
    currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
    imageIterator = getIterator(
            IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 != 0).iterator(), imageFactory);
    maskIterator = getIterator(
            IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 == 0).iterator(), maskFactory);
  }

  @Override
  public void setLister(
          FileLister lister) throws IOException, FormatException {
    this.lister = lister.getFiles().iterator();
    initializeNewReader();
  }

  @Override
  public boolean hasNext() {
    return imageIterator.hasNext();
  }

  @Override
  public Image<T> next() {

    try {
      SCIFIOImgPlus<NativeBoolType> mask = maskIterator.next();
      SCIFIOImgPlus<T> planes = imageIterator.next();

      Image<T> image = createImage(currentReader, currentIndex, planes, mask);
      currentIndex++;

      if (!imageIterator.hasNext() & lister.hasNext()) {
        currentIndex = 0;

        // close current reader
        currentReader.close();

        // initialize new reader
        initializeNewReader();
      }

      boolean valid = validator.validate(image);
      if (!valid)
        return null;

      return image;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      log.error("Format exception " + currentReader.getMetadata().getDatasetName());
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public void addChannel(Long channel) {
    this.channels.add(channel);
  }

  @Override
  public void setImageLimit(int imageLimit) {
    this.imageLimit = imageLimit;
  }

  @Override
  public void setStartIndex(int index) {
    this.currentIndex = index;
  }

}
