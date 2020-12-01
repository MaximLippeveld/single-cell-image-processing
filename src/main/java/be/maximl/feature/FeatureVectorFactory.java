package be.maximl.feature;

import be.maximl.data.Image;
import net.imagej.ops.OpService;
import net.imagej.ops.features.haralick.HaralickNamespace;
import net.imagej.ops.features.zernike.ZernikeNamespace;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation2D;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Double.NaN;

public class FeatureVectorFactory<T extends RealType<T>> {

    final private OpService opService;
    final private Map<String, Function<Iterable<T>, Double>> iterableFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<T>, Double>> iterableIntervalFeatureFunctions = new HashMap<>();
    final private Map<String, Function<Polygon2D, Double>> polygonFeatureFunctions = new HashMap<>();

    public FeatureVectorFactory(OpService opService, List<String> featuresToCompute) {
        this.opService = opService;

        // intensity features
        iterableFeatureFunctions.put("geometricMean", s -> opService.stats().geometricMean(s).getRealDouble());
        iterableFeatureFunctions.put("harmonicMean", s -> opService.stats().harmonicMean(s).getRealDouble());
        iterableFeatureFunctions.put("stdDev", s -> opService.stats().stdDev(s).getRealDouble());
        iterableFeatureFunctions.put("median", s -> opService.stats().median(s).getRealDouble());
        iterableFeatureFunctions.put("sum", s -> opService.stats().sum(s).getRealDouble());
        iterableFeatureFunctions.put("min", s -> opService.stats().min(s).getRealDouble());
        iterableFeatureFunctions.put("max", s -> opService.stats().max(s).getRealDouble());
        iterableFeatureFunctions.put("kurtosis", s -> opService.stats().kurtosis(s).getRealDouble());
        iterableFeatureFunctions.put("skewness", s -> opService.stats().skewness(s).getRealDouble());
        iterableFeatureFunctions.put("moment3AboutMean", s -> opService.stats().moment3AboutMean(s).getRealDouble());

        HaralickNamespace haralick = opService.haralick();
        for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
            iterableIntervalFeatureFunctions.put(
                    "haralickContrast" + orientation,
                    s -> haralick.contrast(s, 50, 5, orientation).getRealDouble()
            );
            iterableIntervalFeatureFunctions.put(
                    "haralickCorrelation" + orientation,
                    s -> haralick.correlation(s, 50, 5, orientation).getRealDouble()
            );
            iterableIntervalFeatureFunctions.put(
                    "haralickEntropy" + orientation,
                    s -> haralick.entropy(s, 50, 5, orientation).getRealDouble()
            );
        }

        ZernikeNamespace zernike = opService.zernike();
        iterableIntervalFeatureFunctions.put("zernikeMagnitude", s -> zernike.magnitude(s, 3, 1).getRealDouble());
        iterableIntervalFeatureFunctions.put("zernikePhase", s -> zernike.phase(s, 3, 1).getRealDouble());

        // geometry features
        polygonFeatureFunctions.put("eccentricity", s -> opService.geom().eccentricity(s).getRealDouble());
        polygonFeatureFunctions.put("circularity", s -> opService.geom().circularity(s).getRealDouble());
        polygonFeatureFunctions.put("roundness", s -> opService.geom().roundness(s).getRealDouble());
        polygonFeatureFunctions.put("convexity", s -> opService.geom().convexity(s).getRealDouble());
        polygonFeatureFunctions.put("size", s -> opService.geom().size(s).getRealDouble());
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

        public <S> void computeFeature(
                String key,
                int channel,
                Function<S, Double> func,
                S slice,
                boolean compute) {
            if(compute) {
                add(key, channel, func.apply(slice));
            } else {
                add(key, channel, NaN);
            }
        }
    }

    public FeatureVector computeVector(Image<T> img) {

        FeatureVector vec = new FeatureVector();
        vec.add("file", img.getFilename());
        vec.add("directory", img.getDirectory());
        vec.add("id", img.getId());

        RandomAccessibleInterval<T> libImg = img.getImg();

        IterableInterval<T> slice;
        Integer channel;
        for (int i = 0; i<img.getChannels().size(); i++) {

            // create an iterator that only samples points within the mask
            MaskInterval mask = img.getMask(i);
            boolean compute = Regions.countTrue(Masks.toRandomAccessibleInterval(mask)) > 0;
            slice = Regions.sample(img.getMask(i), Views.hyperSlice(libImg, 2, i));
            channel = img.getChannels().get(i);

            for(Map.Entry<String, Function<Iterable<T>, Double>> entry : iterableFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), slice, compute);
            }
            for(Map.Entry<String, Function<IterableInterval<T>, Double>> entry : iterableIntervalFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), slice, compute);
            }

            Polygon2D polygon = opService.geom().contour(Masks.toRandomAccessibleInterval(mask), false);
            for(Map.Entry<String, Function<Polygon2D, Double>> entry : polygonFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), polygon, compute);
            }
        }

        return vec;
    }
}
