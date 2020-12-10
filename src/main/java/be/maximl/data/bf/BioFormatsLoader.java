package be.maximl.data.bf;

import be.maximl.data.*;
import be.maximl.data.validators.Validator;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;


public class BioFormatsLoader<T extends RealType<T>, S extends BooleanType<S>> implements Loader<T, S> {
  private static final long serialVersionUID = 4598175080399877334L;
  private final LogService log;
  private Iterator<File> lister;
  final private ArrayList<Integer> channels = new ArrayList<>();
  final private SCIFIO scifio = new SCIFIO();
  final private Validator<T, S> validator;
  private int imageLimit = -1;

  private Reader currentReader;
  private int currentFinalIndex;
  private int currentIndex = 0;

  public BioFormatsLoader(LogService log, Validator<T, S> validator) {
    this.log = log;
    this.validator = validator;
  }

  @Override
  public Image<T, S> imageFromReader(Reader reader, int index) throws IOException, FormatException {

    int imgIndex;
    int maskIndex;
    int pointsInPlane;
    Plane imgPlane;
    Plane maskPlane;
    short[] flatData;
    boolean[] maskData;
    short[] planeTmp;
    Image<T, S> image;

    imgIndex = index;
    maskIndex = index+1;

    image = new BioFormatsImage<>(imgIndex/2);
    image.setDirectory(reader.getMetadata().getSourceLocation().getURI().getPath());
    image.setFilename(reader.getMetadata().getSourceLocation().getName());
    image.setExtension(reader.getFormatName());
    image.setChannels(channels);

    imgPlane = reader.openPlane(imgIndex, channels.get(0));
    image.setPlaneLengths(imgPlane.getLengths());
    image.setSize(imgPlane.getBytes().length * channels.size());

    pointsInPlane = imgPlane.getBytes().length / 2;

    flatData = new short[channels.size()*pointsInPlane];
    planeTmp = new short[pointsInPlane];
    maskData = new boolean[channels.size()*pointsInPlane];

    for (int j = 0; j<channels.size(); j++) {

      maskPlane = reader.openPlane(maskIndex, channels.get(j));
      for (int k = 0; k<pointsInPlane; k++) {
        maskData[k+j*pointsInPlane] = maskPlane.getBytes()[k] > 0;
      }

      if (j > 0) {
        // the plane at j=0 was already loaded in
        imgPlane = reader.openPlane(imgIndex, channels.get(j));
      }
      ByteBuffer.wrap(imgPlane.getBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(planeTmp);
      System.arraycopy(planeTmp, 0, flatData, j * pointsInPlane, pointsInPlane);
    }

    image.setPlanes((Img<T>) ArrayImgs.unsignedShorts(flatData, image.getDimensions()));
    image.setMasks((Img<S>) ArrayImgs.booleans(maskData, image.getDimensions()));

    return image;
  }

  @Override
  public void addChannel(int channel) {
    this.channels.add(channel);
  }

  @Override
  public void setImageLimit(int imageLimit) {
    this.imageLimit = imageLimit;
  }

  @Override
  public void setStartIndex(int index) {
    this.currentIndex = index*2;
  }

  @Override
  public void setLister(
          FileLister lister) throws IOException, FormatException {
    this.lister = lister.getFiles().iterator();
    currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
    currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
  }

  @Override
  public boolean hasNext() {
    return currentIndex < currentFinalIndex;
  }

  @Override
  public Image<T, S> next() {

    try {
      Image<T, S> image = imageFromReader(currentReader, currentIndex);
      currentIndex += 2;

      if (currentIndex >= currentFinalIndex) {
        if (lister.hasNext()) {
          currentIndex = 0;

          // initialize new reader
          FileLocation loc = new FileLocation(lister.next());
          currentReader = scifio.initializer().initializeReader(loc);
          currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
        }
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

}
