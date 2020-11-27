package be.maximl.data.bf;

import be.maximl.data.Image;
import be.maximl.data.RecursiveExtensionFilteredLister;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;


public class BioFormatsLoader implements be.maximl.data.Loader {
  private static final long serialVersionUID = 4598175080399877334L;
  private final LogService log;
  private Iterator<File> lister;
  final private ArrayList<Integer> channels = new ArrayList<>();
  final private SCIFIO scifio = new SCIFIO();
  private int imageLimit = -1;

  private Reader currentReader;
  private int currentFinalIndex;
  private int currentIndex = 0;

  public BioFormatsLoader(LogService log) {
    this.log = log;
  }

  @Override
  public Image imageFromReader(Reader reader, int index) throws IOException, FormatException {

    int imgIndex;
    int maskIndex;
    int pointsInPlane;
    Plane imgPlane;
    Plane maskPlane;
    short[] flatData;
    boolean[] maskData;
    short[] planeTmp;
    Image image;

    imgIndex = index;
    maskIndex = index+1;

    image = new BioFormatsImage(imgIndex/2);
    image.setDirectory(reader.getMetadata().getSourceLocation().getName());
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
      for (int k = 0; k<maskPlane.getBytes().length/2; k++) {
        maskData[k+j*pointsInPlane] = maskPlane.getBytes()[k] > 0;
      }

      if (j > 0) {
        // the plane at j=0 was already loaded in
        imgPlane = reader.openPlane(imgIndex, channels.get(j));
      }
      ByteBuffer.wrap(imgPlane.getBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(planeTmp);
      System.arraycopy(planeTmp, 0, flatData, j * pointsInPlane, pointsInPlane);
    }

    image.setPlanes(flatData);
    image.setMasks(maskData);

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
  public void setLister(
          RecursiveExtensionFilteredLister lister) throws IOException, FormatException {
    this.lister = lister.getFiles().iterator();
    currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
    currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
  }

  @Override
  public boolean hasNext() {
    return currentIndex < currentFinalIndex;
  }

  @Override
  public Image next() {

    try {
      Image image = imageFromReader(currentReader, currentIndex);

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

      return image;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      e.printStackTrace();
    }

    return null;
  }

}
