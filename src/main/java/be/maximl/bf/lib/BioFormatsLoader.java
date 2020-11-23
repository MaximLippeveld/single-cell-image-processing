package be.maximl.bf.lib;

import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import org.scijava.io.location.FileLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class BioFormatsLoader implements Iterator<BioFormatsImage> {
  private static final long serialVersionUID = 4598175080399877334L;
  private static transient Logger log =
      LoggerFactory.getLogger(BioFormatsLoader.class);
  private Iterator<File> lister;
  final private ArrayList<Integer> channels = new ArrayList<>();
  final private SCIFIO scifio = new SCIFIO();
  private int imageLimit = -1;

  private Reader currentReader;
  private int currentFinalIndex;
  private int currentIndex = 0;

  public BioFormatsImage imageFromReader(Reader reader, int index) throws IOException, FormatException {

    int imgIndex;
    int maskIndex;
    int shortsInPlane;
    Plane imgPlane;
    short[] flatData;
    short[] planeData;
    BioFormatsImage image;

    imgIndex = index;
    maskIndex = index+1;

    image = new BioFormatsImage();
    image.setDirectory(reader.getMetadata().getSourceLocation().getName());
    image.setFilename(reader.getMetadata().getSourceLocation().getName());
    image.setExtension(reader.getFormatName());
    image.setChannels(channels);

    imgPlane = reader.openPlane(imgIndex, channels.get(0));
    image.setPlaneLengths(imgPlane.getLengths());
    image.setSize(imgPlane.getBytes().length * channels.size());

    shortsInPlane = imgPlane.getBytes().length / 2;

    flatData = new short[channels.size()*shortsInPlane];
    planeData = new short[shortsInPlane];

    for (int j = 0; j<channels.size(); j++) {

      if (j > 1) { imgPlane = reader.openPlane(imgIndex, channels.get(j)); }
      ByteBuffer.wrap(imgPlane.getBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(planeData);
      System.arraycopy(planeData, 0, flatData, j * shortsInPlane, shortsInPlane);
    }

    image.setPlanes(flatData);

    return image;
  }

  public void addChannel(int channel) {
    this.channels.add(channel);
  }

  public void setImageLimit(int imageLimit) {
    this.imageLimit = imageLimit;
  }

  public void setLister (
      RecursiveExtensionFilteredLister lister) throws IOException, FormatException {
    this.lister = lister.getFiles().iterator();
    currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
    currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
  }

  @Override
  public boolean hasNext() {
    if (currentIndex < currentFinalIndex - 2) {
      return true;
    }
    return lister.hasNext();
  }

  @Override
  public BioFormatsImage next() {

    try {
      BioFormatsImage image = imageFromReader(currentReader, currentIndex);

      currentIndex += 2;

      if (currentIndex == currentFinalIndex) {
        currentIndex = 0;

        // initialize new reader
        FileLocation loc = new FileLocation(lister.next());
        currentReader = scifio.initializer().initializeReader(loc);
        currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
      }

      return image;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      e.printStackTrace();
    }

    return null;
  }

  public List<BioFormatsImage> collectData() {
    return new ArrayList<>();
  }
}
