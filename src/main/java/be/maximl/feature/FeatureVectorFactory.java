package be.maximl.feature;

import be.maximl.data.Image;
import net.imagej.ops.OpService;
import net.imagej.ops.features.haralick.HaralickNamespace;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation2D;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Double.NaN;

public class FeatureVectorFactory {

    final private OpService opService;

    public FeatureVectorFactory(OpService opService) {
        this.opService = opService;
    }

    public static class FeatureVector {
        private final Map<String, String> map = new HashMap<>();

        public void add(String key, int channel, double value) {
            map.put("feat:"+ key + "-" + channel, Double.toString(value));
        }

        public void add(String key, double value) {
            map.put("feat:"+key, Double.toString(value));
        }

        public void add(String key, String value) {
            map.put("meta:"+key, value);
        }

        public void add(String key, int value) {
            map.put("meta:"+key, Integer.toString(value));
        }

        public Map<String, String> getMap() {
            return map;
        }

        public String[] getLine() {
            return map.values().toArray(new String[0]);
        }

        public <T> void computeFeature(
                String key,
                int channel,
                Function<T, Double> func,
                T slice,
                boolean compute) {
            if(compute) {
                add(key, channel, func.apply(slice));
            } else {
                add(key, channel, NaN);
            }
        }
    }

    public FeatureVector computeVector(Image img) {

        FeatureVector vec = new FeatureVector();
        vec.add("file", img.getFilename());
        vec.add("directory", img.getDirectory());
        vec.add("id", img.getId());

        RandomAccessibleInterval<UnsignedShortType> libImg = img.getImg();

        IterableInterval<UnsignedShortType> slice;
        Integer channel;
        for (int i = 0; i<img.getChannels().size(); i++) {

            // create an iterator that only samples points within the mask
            MaskInterval mask = img.getMask(i);
            boolean compute = Regions.countTrue(Masks.toRandomAccessibleInterval(mask)) > 0;
            slice = Regions.sample(img.getMask(i), Views.hyperSlice(libImg, 2, i));
            channel = img.getChannels().get(i);

            // intensity features
            vec.computeFeature("geometricMean", channel, s -> opService.stats().geometricMean(s).getRealDouble(), slice, compute);
            vec.computeFeature("harmonicMean", channel, s -> opService.stats().harmonicMean(s).getRealDouble(), slice, compute);
            vec.computeFeature("stdDev", channel, s -> opService.stats().stdDev(s).getRealDouble(), slice, compute);
            vec.computeFeature("median", channel, s -> opService.stats().median(s).getRealDouble(), slice, compute);
            vec.computeFeature("sum", channel, s -> opService.stats().sum(s).getRealDouble(), slice, compute);
            vec.computeFeature("min", channel, s -> opService.stats().min(s).getRealDouble(), slice, compute);
            vec.computeFeature("max", channel, s -> opService.stats().max(s).getRealDouble(), slice, compute);
            vec.computeFeature("kurtosis", channel, s -> opService.stats().kurtosis(s).getRealDouble(), slice, compute);
            vec.computeFeature("skewness", channel, s -> opService.stats().skewness(s).getRealDouble(), slice, compute);
            vec.computeFeature("moment3AboutMean", channel, s -> opService.stats().moment3AboutMean(s).getRealDouble(), slice, compute);

            // geometry features
            Polygon2D polygon = opService.geom().contour(Masks.toRandomAccessibleInterval(mask), false);
            vec.computeFeature("eccentricity", channel, s -> opService.geom().eccentricity(s).getRealDouble(), polygon, compute);
            vec.computeFeature("circularity", channel, s -> opService.geom().circularity(s).getRealDouble(), polygon, compute);
            vec.computeFeature("roundness", channel, s -> opService.geom().roundness(s).getRealDouble(), polygon, compute);
            vec.computeFeature("convexity", channel, s -> opService.geom().convexity(s).getRealDouble(), polygon, compute);
            vec.computeFeature("size", channel, s -> opService.geom().size(s).getRealDouble(), polygon, compute);

            HaralickNamespace haralick = opService.haralick();
            for ( MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
                vec.computeFeature(
                        "haralickContrast" + orientation, channel,
                        s -> haralick.contrast(s, 50, 2, orientation).getRealDouble(),
                        slice, compute
                );
                vec.computeFeature(
                        "haralickCorrelation" + orientation, channel,
                        s -> haralick.correlation(s, 50, 2, orientation).getRealDouble(),
                        slice, compute
                );
            }
        }

        return vec;
    }

}
