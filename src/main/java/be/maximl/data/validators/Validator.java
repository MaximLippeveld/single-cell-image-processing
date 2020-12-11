package be.maximl.data.validators;

import be.maximl.data.Image;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;

import java.util.List;

public interface Validator<T extends NativeType<T>> {

    boolean validate(Image<T> image);

    int getInvalidCount();

    List<Integer> getInvalidIds();

}
