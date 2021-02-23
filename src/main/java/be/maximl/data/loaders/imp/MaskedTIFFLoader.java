package be.maximl.data.loaders.imp;

import be.maximl.data.loaders.MaskedLoader;
import be.maximl.data.validators.Validator;
import io.scif.FormatException;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MaskedTIFFLoader<T extends NativeType<T> & RealType<T>> extends MaskedLoader<T> {

    public MaskedTIFFLoader(LogService log, int imageLimit, List<Long> channels, Iterator<File> lister, Iterator<File> maskLister, SCIFIO scifio, Validator<T> validator) {
        super(log, imageLimit, channels, lister, maskLister, scifio, validator);
    }

    @Override
    protected void initializeNewReader() throws IOException, FormatException {
        super.initializeNewReader();
        maskReader = scifio.initializer().initializeReader(new FileLocation(maskLister.next()));
    }

    @Override
    protected Iterator<Img<NativeBoolType>> initializeNewMaskIterator() {
        ImgFactory<NativeBoolType> factory = new ArrayImgFactory<>(new NativeBoolType());
        return getIterator(Collections.singleton(0).iterator(), factory, maskReader);
    }

    @Override
    protected Iterator<Img<T>> initializeNewImageIterator() {
        ImgFactory<T> factory = new ArrayImgFactory<>(getType());
        return getIterator(Collections.singleton(0).iterator(), factory, currentReader);
    }
}
