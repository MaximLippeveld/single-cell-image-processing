package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class FeatureVectorFactory {

    final private ImageJ ij = new ImageJ();

    public static class FeatureVector {
        final private double geometricMean;
        final private double variance;
        final private double sum;
        final private double min;
        final private double max;
        final private double harmonicMean;
        final private double kurtosis;
        final private double skewness;
        final private double moment3AboutMean;
        final private String file;

        public FeatureVector(double geometricMean, double variance, double sum, double min, double max, double harmonicMean, double kurtosis, double skewness, double moment3AboutMean, String file) {
            this.geometricMean = geometricMean;
            this.variance = variance;
            this.sum = sum;
            this.min = min;
            this.max = max;
            this.harmonicMean = harmonicMean;
            this.kurtosis = kurtosis;
            this.skewness = skewness;
            this.moment3AboutMean = moment3AboutMean;
            this.file = file;
        }
    }

    public FeatureVector computeVector(BioFormatsImage img) {

        Iterable<UnsignedShortType> it = img.getImg();

        return new FeatureVector(
                ij.op().stats().geometricMean(it).getRealDouble(),
                ij.op().stats().variance(it).getRealDouble(),
                ij.op().stats().sum(it).getRealDouble(),
                ij.op().stats().min(it).getRealDouble(),
                ij.op().stats().max(it).getRealDouble(),
                ij.op().stats().harmonicMean(it).getRealDouble(),
                ij.op().stats().kurtosis(it).getRealDouble(),
                ij.op().stats().skewness(it).getRealDouble(),
                ij.op().stats().moment3AboutMean(it).getRealDouble(),
                img.getFilename()
                );
    }

}
