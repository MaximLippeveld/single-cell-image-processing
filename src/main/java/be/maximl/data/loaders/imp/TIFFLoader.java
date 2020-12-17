package be.maximl.data.loaders.imp;

import be.maximl.data.loaders.Loader;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.log.LogService;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TIFFLoader<T extends NativeType<T> & RealType<T>> extends Loader<T> {

    protected TIFFLoader(Iterator<File> lister, List<Long> channels, LogService log, SCIFIO scifio) {
        super(lister, channels, 1, log, scifio);
    }

    @Override
    protected Iterator<Img<T>> initializeNewIterator() {
        ImgFactory<T> factory = new ArrayImgFactory<>(getType());
        return getIterator(Collections.singleton(0).iterator(), factory);
    }
}
