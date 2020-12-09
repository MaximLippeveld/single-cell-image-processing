package be.maximl.data.validators;

import be.maximl.data.Image;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import inra.ijpb.binary.conncomp.ConnectedComponentsLabeling;
import inra.ijpb.binary.conncomp.FloodFillComponentsLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


public class ConnectedComponentsValidator<T extends RealType<T>, S extends NativeType<S>> implements Validator<T,S> {

    private int invalid = 0;

    @Override
    public boolean validate(Image<T,S> image) {
        ConnectedComponentsLabeling labeling = new FloodFillComponentsLabeling(1, 8);
        long[] dims = image.getDimensions();

        for (int i = 0; i<image.getChannels().size(); i++) {

            boolean[] mask = image.getMaskAsBooleanArray(i);
            byte[] maskBytes = new byte[mask.length];
            for (int b = 0; b<mask.length; b++) {
                maskBytes[b] = (byte)(mask[b]?1:0);
            }

            BinaryProcessor bp = new BinaryProcessor(new ByteProcessor((int)dims[0], (int)dims[1], maskBytes));
            int[] histogram = labeling.computeLabels(bp).getHistogram();

            boolean count = false;
            for (int j = 1; j<histogram.length; j++) { // start at 1 because 0 is background
                if (histogram[j] > 0) {
                    if (count) {
                        invalid++;
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
}
