package be.maximl.data;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.roi.MaskInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.Serializable;
import java.util.List;

public interface Image<T extends RealType<T>> extends Serializable {

    int getId();

    short[] getPlanes();

    void setPlanes(short[] planes);

    void setMasks(boolean[] masks);

    void setPlaneLengths(long[] planeLengths);

    RandomAccessibleInterval<T> getImg();

    ArrayImg<NativeBoolType, BooleanArray> getMaskImg();

    MaskInterval getMask(int pos);

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
}
