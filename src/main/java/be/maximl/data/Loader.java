package be.maximl.data;

import io.scif.FormatException;
import io.scif.Reader;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.util.Iterator;

public interface Loader<T extends NativeType<T>> extends Iterator<Image<T>> {

    void addChannel(Long channel);

    void setImageLimit(int imageLimit);

    void setStartIndex(int index);

    void setLister (FileLister lister) throws IOException, FormatException;
}
