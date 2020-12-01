package be.maximl.data;

import io.scif.FormatException;
import io.scif.Reader;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;
import java.util.Iterator;

public interface Loader<T extends RealType<T>> extends Iterator<Image<T>> {
    Image<T> imageFromReader(Reader reader, int index) throws IOException, FormatException;

    void addChannel(int channel);

    void setImageLimit(int imageLimit);

    void setLister (
            RecursiveExtensionFilteredLister lister) throws IOException, FormatException;
}
