package be.maximl.data.loaders;

import be.maximl.data.validators.Validator;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import org.scijava.log.LogService;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class MaskedCIFLoader<T extends NativeType<T> & RealType<T>> extends MaskedLoader<T> {

    public MaskedCIFLoader(LogService log, int imageLimit, List<Long> channels, Iterator<File> lister, SCIFIO scifio, Validator<T> validator, T imageType) {
        super(log, imageLimit, channels, lister, scifio, validator, imageType);
    }

    @Override
    protected Iterator<Img<NativeBoolType>> initializeNewMaskIterator() {
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 != 0).iterator(), maskFactory);
    }

    @Override
    protected Iterator<Img<T>> initializeNewImageIterator() {
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 == 0).iterator(), factory);
    }
}
