package be.maximl.data.validators;

import be.maximl.data.Image;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface Validator<T extends RealType<T>, S extends NativeType<S>> {

    boolean validate(Image<T, S> image);

    int getInvalidCount();

}
