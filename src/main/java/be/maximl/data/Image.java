package be.maximl.data;

import ij.process.ImageProcessor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.roi.MaskInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class which holds image-data to be processed.
 *
 * @param <T>
 * @param <S>
 */
public interface Image<T extends RealType<T>, S extends BooleanType<S>> extends Serializable {

    /**
     * @return unique identifier
     */
    int getId();

    void setPlanes(Img<T> planes);

    void setMasks(Img<S> masks);

    void setPlaneLengths(long[] planeLengths);

    RandomAccessibleInterval<T> getRAI();

    ImgFactory<T> getFactory();

    MaskInterval getMaskInterval(int pos);

    IterableInterval<S> getMaskAsIterableInterval(int pos);

    String getDirectory();

    String getExtension();

    String getFilename();

    long getSize();

    List<Integer> getChannels();

    void setChannels(List<Integer> channels);

    void setDirectory(String directory);

    void setExtension(String extension);

    void setFilename(String filename);

    void setSize(long size);

    ImageProcessor getMaskAsImageProcessor(int i);

    long[] getDimensions();
}
