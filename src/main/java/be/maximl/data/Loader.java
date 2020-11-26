package be.maximl.data;

import io.scif.FormatException;
import io.scif.Reader;

import java.io.IOException;
import java.util.Iterator;

public interface Loader extends Iterator<Image> {
    Image imageFromReader(Reader reader, int index) throws IOException, FormatException;

    void addChannel(int channel);

    void setImageLimit(int imageLimit);

    void setLister (
            RecursiveExtensionFilteredLister lister) throws IOException, FormatException;
}
