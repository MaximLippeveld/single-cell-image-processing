package be.maximl.bf.lib;

import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import net.imglib2.img.array.ArrayImgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class BFUtils {
  private static transient Logger log = LoggerFactory.getLogger(
      BFUtils.class);

  public static List<BioFormatsImage>
      processFromFilename(Reader reader, int n, ArrayList<Integer> channels) throws IOException, FormatException {

    int imgIndex;
    int maskIndex;
    int shortsInPlane;
    Plane imgPlane;
    short[] flatData;
    short[] planeData;
    BioFormatsImage image;

    List<BioFormatsImage> list = new ArrayList<>();

    if (n == -1) {
      n = reader.getImageCount();
    }

    for (int i=0; i<n; i+=2) {
      imgIndex = i;
      maskIndex = i+1;

      image = new BioFormatsImage();
      image.setDirectory(reader.getMetadata().getSourceLocation().getName());
      image.setFilename(reader.getMetadata().getSourceLocation().getName());
      image.setExtension(reader.getFormatName());
      image.setChannels((int)reader.getPlaneCount(i));

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

      list.add(image);
    }

    return list;
  }

}
