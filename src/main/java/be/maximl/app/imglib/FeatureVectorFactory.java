package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class FeatureVectorFactory {

    final private ImageJ ij = new ImageJ();

    public static class FeatureVector {
        private double geometricMean;
        private double variance;
        private double sum;
        private double min;
        private double max;

        public FeatureVector(double geometricMean, double variance, double sum, double min, double max) {
            this.geometricMean = geometricMean;
            this.variance = variance;
            this.sum = sum;
            this.min = min;
            this.max = max;
        }
    }

    public FeatureVector computeVector(BioFormatsImage img) {

        Iterable<UnsignedShortType> it = img.getImg();

        return new FeatureVector(
                ij.op().stats().geometricMean(it).getRealDouble(),
                ij.op().stats().variance(it).getRealDouble(),
                ij.op().stats().sum(it).getRealDouble(),
                ij.op().stats().min(it).getRealDouble(),
                ij.op().stats().max(it).getRealDouble()
        );
    }

}
