package be.maximl.data;

import ij.process.ImageProcessor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.roi.MaskInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.IntervalView;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class which holds image-data to be processed.
 *
 * @param <T>
 */
public interface Image<T extends NativeType<T>> extends Serializable {

    /**
     * @return unique identifier
     */
    int getId();

    void setPlanes(Img<T> planes);

    void setMasks(Img<NativeBoolType> masks);

    void setAxesLengths (long[] axesLengths);

    RandomAccessibleInterval<T> getRAI();

    ImgFactory<T> getFactory();

    MaskInterval getMaskInterval(int pos);

    IterableInterval<NativeBoolType> getMaskAsIterableInterval(int pos);

    String getDirectory();

    String getExtension();

    String getFilename();

    List<Long> getChannels();

    void setChannels(List<Long> channels);

    void setDirectory(String directory);

    void setExtension(String extension);

    void setFilename(String filename);

    ImageProcessor getMaskAsImageProcessor(int i);

    long[] getDimensions();
}
