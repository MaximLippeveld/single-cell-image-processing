package be.maximl.data;

import io.scif.FormatException;
import io.scif.Reader;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.util.Iterator;

public interface Loader<T extends RealType<T>, S extends NativeType<S>> extends Iterator<Image<T, S>> {
    Image<T, S> imageFromReader(Reader reader, int index) throws IOException, FormatException;

    void addChannel(int channel);

    void setImageLimit(int imageLimit);

    void setStartIndex(int index);

    void setLister (FileLister lister) throws IOException, FormatException;
}
