package be.maximl.data.validators;

import be.maximl.data.Image;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.conncomp.ConnectedComponentsLabeling;
import inra.ijpb.binary.conncomp.FloodFillComponentsLabeling;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.List;


public class ConnectedComponentsValidator<T extends RealType<T>, S extends BooleanType<S>> implements Validator<T,S> {

    private int invalid = 0;
    final private List<Integer> invalidList = new ArrayList<>();

    @Override
    public boolean validate(Image<T,S> image) {
        ConnectedComponentsLabeling labeling = new FloodFillComponentsLabeling(1, 8);

        for (int i = 0; i<image.getChannels().size(); i++) {
            ImageProcessor mask = image.getMaskAsImageProcessor(i);
            BinaryProcessor bp = new BinaryProcessor(new ByteProcessor(mask, false));
            int[] histogram = labeling.computeLabels(bp).getHistogram();

            boolean count = false;
            for (int j = 1; j<histogram.length; j++) { // start at 1 because 0 is background
                if (histogram[j] > 0) {
                    if (count) {
                        invalid++;
                        invalidList.add(image.getId());
                        return false;
                    }
                    count = true;
                }
            }
        }

        return true;
    }

    @Override
    public int getInvalidCount() {
        return invalid;
    }

    @Override
    public List<Integer> getInvalidIds() {
        return invalidList;
    }
}
