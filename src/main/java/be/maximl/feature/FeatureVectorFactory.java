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
import org.scijava.log.LogService;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class FeatureVectorFactory<T extends RealType<T>, S extends NativeType<S>> {

    final private OpService opService;
    final private LogService logService;
    final private Map<String, Function<Iterable<T>, Double>> iFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<T>, Double>> iiFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<S>, Double>> iiMaskFeatureFunctions = new HashMap<>();
    final private Map<String, Function<Polygon2D, Double>> pFeatureFunctions = new HashMap<>();
    final private Map<String, Function<RandomAccessibleInterval<T>, Double>> raiFeatureFunctions = new HashMap<>();

    final public static List<String> FEATURESET_SMALL = Arrays.asList("stdDev", "median", "min", "max", "size", "eccentricity");
    private final List<String> featuresToCompute;
    private final boolean all;
    private int featCounter = 0;

    private <U> BiConsumer<String, Function<U, Double>> addFunc(Map<String, Function<U, Double>> map) {
        return (key, func) -> {
            String p = key.split("-")[0];
            if (all | featuresToCompute.contains(p)) {
                featCounter++;
                map.put(key, func);
                if (!all)
                    featuresToCompute.remove(p);
            }
        };
    }

    public FeatureVectorFactory(OpService opService, LogService logService, List<String> featuresToCompute, boolean all) {
        this.opService = opService;
        this.logService = logService;
        this.featuresToCompute = featuresToCompute;
        this.all = all;

        BiConsumer<String, Function<Iterable<T>, Double>> iFuncAdder = addFunc(iFeatureFunctions);
        BiConsumer<String, Function<IterableInterval<T>, Double>> iiFuncAdder = addFunc(iiFeatureFunctions);
        BiConsumer<String, Function<IterableInterval<S>, Double>> iiMaskFuncAdder = addFunc(iiMaskFeatureFunctions);
        BiConsumer<String, Function<Polygon2D, Double>> pFuncAdder = addFunc(pFeatureFunctions);
        BiConsumer<String, Function<RandomAccessibleInterval<T>, Double>> raiFuncAdder = addFunc(raiFeatureFunctions);

        // intensity features
        iFuncAdder.accept("mean", s -> opService.stats().mean(s).getRealDouble());
        iFuncAdder.accept("geometricMean", s -> opService.stats().geometricMean(s).getRealDouble());
        iFuncAdder.accept("harmonicMean", s -> opService.stats().harmonicMean(s).getRealDouble());
        iFuncAdder.accept("stdDev", s -> opService.stats().stdDev(s).getRealDouble());
        iFuncAdder.accept("median", s -> opService.stats().median(s).getRealDouble());
        iFuncAdder.accept("sum", s -> opService.stats().sum(s).getRealDouble());
        iFuncAdder.accept("min", s -> opService.stats().min(s).getRealDouble());
        iFuncAdder.accept("max", s -> opService.stats().max(s).getRealDouble());
        iFuncAdder.accept("kurtosis", s -> opService.stats().kurtosis(s).getRealDouble());
        iFuncAdder.accept("skewness", s -> opService.stats().skewness(s).getRealDouble());
        iFuncAdder.accept("moment3AboutMean", s -> opService.stats().moment3AboutMean(s).getRealDouble());

        // texture
        HaralickNamespace haralick = opService.haralick();
        if (featuresToCompute.contains("haralickContrast")) {
            featuresToCompute.remove("haralickContrast");
            featuresToCompute.addAll(Arrays.stream(MatrixOrientation2D.values()).map(o -> "haralickContrast" + o).collect(Collectors.toList()));
        }
        for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
            iiFuncAdder.accept(
                    "haralickContrast" + orientation,
                    s -> haralick.contrast(s, 50, 5, orientation).getRealDouble()
            );
        }

        if (featuresToCompute.contains("haralickCorrelation")) {
            featuresToCompute.remove("haralickCorrelation");
            featuresToCompute.addAll(Arrays.stream(MatrixOrientation2D.values()).map(o -> "haralickCorrelation" + o).collect(Collectors.toList()));
        }
        for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
            iiFuncAdder.accept(
                    "haralickCorrelation" + orientation,
                    s -> haralick.correlation(s, 50, 5, orientation).getRealDouble()
            );
        }

        if (featuresToCompute.contains("haralickEntropy")) {
            featuresToCompute.remove("haralickEntropy");
            featuresToCompute.addAll(Arrays.stream(MatrixOrientation2D.values()).map(o -> "haralickEntropy" + o).collect(Collectors.toList()));
        }
        for (MatrixOrientation2D orientation : MatrixOrientation2D.values()) {
            iiFuncAdder.accept(
                    "haralickEntropy" + orientation,
                    s -> haralick.entropy(s, 50, 5, orientation).getRealDouble()
            );
        }

        TamuraNamespace tamura = opService.tamura();
        raiFuncAdder.accept("tamuraContrast", s -> tamura.contrast(s).getRealDouble());

        ZernikeNamespace zernike = opService.zernike();
        iiFuncAdder.accept("zernikeMagnitude", s -> zernike.magnitude(s, 3, 1).getRealDouble());
        iiFuncAdder.accept("zernikePhase", s -> zernike.phase(s, 3, 1).getRealDouble());

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
        raiFuncAdder.accept("sobelRMS", func);

        // geometry features
        pFuncAdder.accept("eccentricity", s -> opService.geom().eccentricity(s).getRealDouble());
        pFuncAdder.accept("circularity", s -> opService.geom().circularity(s).getRealDouble());
        pFuncAdder.accept("roundness", s -> opService.geom().roundness(s).getRealDouble());
        pFuncAdder.accept("convexity", s -> opService.geom().convexity(s).getRealDouble());
        pFuncAdder.accept("size", s -> opService.geom().size(s).getRealDouble());
        pFuncAdder.accept("sizeConvexHull", s -> opService.geom().sizeConvexHull(s).getRealDouble());

        iiMaskFuncAdder.accept("sizeMask", s -> opService.geom().size(s).getRealDouble());

        if(!all & (featuresToCompute.size() > 0))
            throw new AssertionError("Not all features in the list were recognized.");

        logService.info("Computing " + featCounter + " features per channel.");
    }

    public static class FeatureVector {
        private final Map<String, Object> map = new HashMap<>();

        public void add(String key, int channel, double value) {
            map.put("feat_"+ key + "_" + channel, value);
        }

        public void add(String key, double value) {
            map.put("feat_"+key, value);
        }

        public void add(String key, String value) {
            map.put("meta_"+key, value);
        }

        public void add(String key, int value) {
            map.put("meta_"+key, value);
        }

        public Map<String, Object> getMap() {
            return map;
        }

        public String[] getLine() {

            String[] res = new String[map.size()];
            int i =0;
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                res[i] = entry.getValue().toString();
                i++;
            }

            return res;
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
