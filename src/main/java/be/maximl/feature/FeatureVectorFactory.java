package be.maximl.feature;

import be.maximl.data.Image;
import net.imagej.ops.OpService;
import net.imagej.ops.features.haralick.HaralickNamespace;
import net.imagej.ops.features.tamura2d.TamuraNamespace;
import net.imagej.ops.features.zernike.ZernikeNamespace;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation2D;
import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.*;
import java.util.function.Function;

import static java.lang.Double.NaN;

public class FeatureVectorFactory<T extends RealType<T>, S extends NativeType<S>> {

    final private OpService opService;
    final private Map<String, Function<Iterable<T>, Double>> iFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<T>, Double>> iiFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<S>, Double>> iiMaskFeatureFunctions = new HashMap<>();
    final private Map<String, Function<Polygon2D, Double>> pFeatureFunctions = new HashMap<>();
    final private Map<String, Function<RandomAccessibleInterval<T>, Double>> raiFeatureFunctions = new HashMap<>();

    final public static List<String> FEATURESET_SMALL = Arrays.asList("stdDev", "median", "min", "max", "size", "eccentricity");

    public FeatureVectorFactory(OpService opService, List<String> featuresToCompute) {
        this.opService = opService;

        // intensity features
        if (featuresToCompute.contains("mean"))
            iFeatureFunctions.put("mean", s -> opService.stats().mean(s).getRealDouble());
        if (featuresToCompute.contains("geometricMean"))
            iFeatureFunctions.put("geometricMean", s -> opService.stats().geometricMean(s).getRealDouble());
        if(featuresToCompute.contains("harmonicMean"))
            iFeatureFunctions.put("harmonicMean", s -> opService.stats().harmonicMean(s).getRealDouble());
        if(featuresToCompute.contains("stdDev"))
            iFeatureFunctions.put("stdDev", s -> opService.stats().stdDev(s).getRealDouble());
        if(featuresToCompute.contains("median"))
            iFeatureFunctions.put("median", s -> opService.stats().median(s).getRealDouble());
        if(featuresToCompute.contains("sum"))
            iFeatureFunctions.put("sum", s -> opService.stats().sum(s).getRealDouble());
        if(featuresToCompute.contains("min"))
            iFeatureFunctions.put("min", s -> opService.stats().min(s).getRealDouble());
        if(featuresToCompute.contains("max"))
            iFeatureFunctions.put("max", s -> opService.stats().max(s).getRealDouble());
        if(featuresToCompute.contains("kurtosis"))
            iFeatureFunctions.put("kurtosis", s -> opService.stats().kurtosis(s).getRealDouble());
        if(featuresToCompute.contains("skewness"))
            iFeatureFunctions.put("skewness", s -> opService.stats().skewness(s).getRealDouble());
        if(featuresToCompute.contains("moment3AboutMean"))
            iFeatureFunctions.put("moment3AboutMean", s -> opService.stats().moment3AboutMean(s).getRealDouble());

        // texture
        HaralickNamespace haralick = opService.haralick();
        if(featuresToCompute.contains("haralickContrast")) {
            for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
                iiFeatureFunctions.put(
                        "haralickContrast" + orientation,
                        s -> haralick.contrast(s, 50, 5, orientation).getRealDouble()
                );
            }
        }
        if(featuresToCompute.contains("haralickCorrelation")) {
            for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
                iiFeatureFunctions.put(
                        "haralickCorrelation" + orientation,
                        s -> haralick.correlation(s, 50, 5, orientation).getRealDouble()
                );
            }
        }
        if(featuresToCompute.contains("haralickEntropy")) {
            for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
                iiFeatureFunctions.put(
                        "haralickEntropy" + orientation,
                        s -> haralick.entropy(s, 50, 5, orientation).getRealDouble()
                );
            }
        }

        TamuraNamespace tamura = opService.tamura();
        if (featuresToCompute.contains("tamuraContrast")) {
            raiFeatureFunctions.put("tamuraContrast", s -> tamura.contrast(s).getRealDouble());
        }

        if (featuresToCompute.contains("zernike")) {
            ZernikeNamespace zernike = opService.zernike();
            iiFeatureFunctions.put("zernikeMagnitude", s -> zernike.magnitude(s, 3, 1).getRealDouble());
            iiFeatureFunctions.put("zernikePhase", s -> zernike.phase(s, 3, 1).getRealDouble());
        }

        if (featuresToCompute.contains("sobelRMS")) {
            Function<RandomAccessibleInterval<T>, Double> func = s -> {
                RandomAccessibleInterval<T> sobel = opService.filter().sobel(s);
                double res = .0;
                double count = 0;
                for (T t : ImgView.wrap(sobel)) {
                    res += Math.pow(t.getRealDouble(), 2);
                    count++;
                }
                return Math.sqrt(res/count);
            };
            raiFeatureFunctions.put("sobelRMS", func);
        }

        // geometry features
        if(featuresToCompute.contains("eccentricity"))
            pFeatureFunctions.put("eccentricity", s -> opService.geom().eccentricity(s).getRealDouble());
        if(featuresToCompute.contains("circularity"))
            pFeatureFunctions.put("circularity", s -> opService.geom().circularity(s).getRealDouble());
        if(featuresToCompute.contains("roundness")) // roundess = aspectRatio
            pFeatureFunctions.put("roundness", s -> opService.geom().roundness(s).getRealDouble());
        if(featuresToCompute.contains("convexity"))
            pFeatureFunctions.put("convexity", s -> opService.geom().convexity(s).getRealDouble());
        if(featuresToCompute.contains("size"))
            pFeatureFunctions.put("size", s -> opService.geom().size(s).getRealDouble());
        if(featuresToCompute.contains("sizeConvexHull"))
            pFeatureFunctions.put("sizeConvexHull", s -> opService.geom().sizeConvexHull(s).getRealDouble());

        if(featuresToCompute.contains("sizeMask"))
            iiMaskFeatureFunctions.put("sizeMask", s -> opService.geom().size(s).getRealDouble());

        int size = pFeatureFunctions.size()
                + iFeatureFunctions.size()
                + iiFeatureFunctions.size()
                + raiFeatureFunctions.size()
                + iiMaskFeatureFunctions.size();
        if(size != featuresToCompute.size())
            throw new AssertionError("Not all features in the list were recognized.");
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

    public FeatureVector computeVector(Image<T,S> img) {

        FeatureVector vec = new FeatureVector();
        vec.add("file", img.getFilename());
        vec.add("directory", img.getDirectory());
        vec.add("id", img.getId());

        RandomAccessibleInterval<T> libImg = img.getImg();
        ImgFactory<T> factory = img.getFactory();

        IterableInterval<T> maskedSlice;
        MaskInterval sliceMask;
        Integer channel;
        boolean compute;
        for (int i = 0; i<img.getChannels().size(); i++) {

            // create an iterator that only samples points within the mask
            sliceMask = img.getMaskInterval(i);
            maskedSlice = Regions.sample(img.getMaskInterval(i), Views.hyperSlice(libImg, 2, i));

            compute = Regions.countTrue(Masks.toRandomAccessibleInterval(sliceMask)) > 0;
            channel = img.getChannels().get(i);

            for(Map.Entry<String, Function<Iterable<T>, Double>> entry : iFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), maskedSlice, compute);
            }
            for(Map.Entry<String, Function<IterableInterval<T>, Double>> entry : iiFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), maskedSlice, compute);
            }

            Polygon2D polygon = opService.geom().contour(Masks.toRandomAccessibleInterval(sliceMask), false);
            for(Map.Entry<String, Function<Polygon2D, Double>> entry : pFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), polygon, compute);
            }

            IterableInterval<S> iiMask = img.getMaskAsIterableInterval(i);
            for(Map.Entry<String, Function<IterableInterval<S>, Double>> entry : iiMaskFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), iiMask, compute);
            }

            RandomAccessibleInterval<T> rai = factory.create(maskedSlice.dimensionsAsLongArray());
            Cursor<T> cursor = maskedSlice.localizingCursor();
            RandomAccess<T> rac = rai.randomAccess();
            while(cursor.hasNext()) {
                cursor.fwd();
                T type = cursor.get();
                if(type.getRealDouble() > 0) {
                    rac.setPosition(cursor);
                    rac.get().set(type);
                }
            }
            for(Map.Entry<String, Function<RandomAccessibleInterval<T>, Double>> entry : raiFeatureFunctions.entrySet()) {
                vec.computeFeature(entry.getKey(), channel, entry.getValue(), rai, compute);
            }
        }

        return vec;
    }
}
