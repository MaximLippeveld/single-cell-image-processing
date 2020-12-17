package be.maximl.data.loaders.imp;

import be.maximl.data.loaders.MaskedLoader;
import be.maximl.data.validators.Validator;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import org.scijava.log.LogService;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class CIFLoader<T extends NativeType<T> & RealType<T>> extends MaskedLoader<T> {

    public CIFLoader(LogService log, int imageLimit, List<Long> channels, Iterator<File> lister, SCIFIO scifio, Validator<T> validator, T type) {
        super(log, imageLimit, channels, lister, scifio, validator, type);
    }

    @Override
    protected Iterator<Img<NativeBoolType>> initializeNewMaskIterator() {
        ImgFactory<NativeBoolType> factory = new ArrayImgFactory<>(new NativeBoolType());
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 != 0).iterator(), factory);
    }

    @Override
    protected Iterator<Img<T>> initializeNewImageIterator() {
        ImgFactory<T> factory = new ArrayImgFactory<>(type);
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 == 0).iterator(), factory);
    }
}
