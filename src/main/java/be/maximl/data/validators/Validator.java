package be.maximl.data.validators;

import be.maximl.data.Image;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.List;

public interface Validator<T extends RealType<T>, S extends BooleanType<S>> {

    boolean validate(Image<T, S> image);

    int getInvalidCount();

    List<Integer> getInvalidIds();

}
