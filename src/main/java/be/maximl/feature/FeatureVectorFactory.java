/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 - 2021 Maxim Lippeveld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package be.maximl.feature;

import be.maximl.data.Image;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.SquareStrel;
import net.imagej.ops.OpService;
import net.imagej.ops.features.haralick.HaralickNamespace;
import net.imagej.ops.features.tamura2d.TamuraNamespace;
import net.imagej.ops.features.zernike.ZernikeNamespace;
import net.imagej.ops.image.cooccurrenceMatrix.MatrixOrientation2D;
import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.log.LogService;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class FeatureVectorFactory<T extends NativeType<T> & RealType<T>> {

    final private OpService opService;
    final private LogService logService;
    final private Map<String, Function<Iterable<T>, Double>> iFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<T>, Double>> iiFeatureFunctions = new HashMap<>();
    final private Map<String, Function<IterableInterval<NativeBoolType>, Double>> iiMaskFeatureFunctions = new HashMap<>();
    final private Map<String, Function<Polygon2D, Double>> pFeatureFunctions = new HashMap<>();
    final private Map<String, Function<RandomAccessibleInterval<T>, Double>> raiFeatureFunctions = new HashMap<>();
    final private Map<String, Function<ImageProcessor, Double>> ipFeatureFunctions = new HashMap<>();

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
        BiConsumer<String, Function<IterableInterval<NativeBoolType>, Double>> iiMaskFuncAdder = addFunc(iiMaskFeatureFunctions);
        BiConsumer<String, Function<Polygon2D, Double>> pFuncAdder = addFunc(pFeatureFunctions);
        BiConsumer<String, Function<RandomAccessibleInterval<T>, Double>> raiFuncAdder = addFunc(raiFeatureFunctions);
        BiConsumer<String, Function<ImageProcessor, Double>> ipFuncAdder = addFunc(ipFeatureFunctions);

        // basic image properties
        iiFuncAdder.accept("width", s -> (double)s.dimension(0));
        iiFuncAdder.accept("height", s -> (double)s.dimension(1));

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
        iFuncAdder.accept("mad", s -> {
            double mean = opService.stats().mean(s).getRealDouble();
            double distSum = .0;
            double count = 0;
            for (T t : s) {
                count++;
                distSum += Math.abs(t.getRealDouble() - mean);
            }
            return distSum / count;
        });

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

        raiFuncAdder.accept("sobelRMS", s -> {
            RandomAccessibleInterval<T> sobel = opService.filter().sobel(s);
            double res = .0;
            double count = 0;
            for (T t : ImgView.wrap(sobel)) {
                res += Math.pow(t.getRealDouble(), 2);
                count++;
            }
            return Math.sqrt(res/count);
        });

        Function<Integer, Function<ImageProcessor, Double>> gradientRMS = i -> s -> {
            Strel se = SquareStrel.fromDiameter(i);
            ImageProcessor grad = Morphology.gradient(s, se);

            float [][] values = grad.getFloatArray();
            float sum = 0;
            for (float[] arr: values) {
                for (float v: arr) {
                    sum+=Math.pow(v, 2);
                }
            }
            return Math.sqrt(sum / (grad.getHeight() * grad.getWidth()));
        };
        List<String> gradientRMSFeatures = featuresToCompute.stream().filter(s -> s.matches("^gradientRMS:[0-9]")).collect(Collectors.toList());
        for (String g : gradientRMSFeatures) {
            int i = Integer.parseInt(g.split(":")[1]);
            ipFuncAdder.accept(g, gradientRMS.apply(i));
        }

        // geometry features
        pFuncAdder.accept("eccentricity", s -> opService.geom().eccentricity(s).getRealDouble());
        pFuncAdder.accept("circularity", s -> opService.geom().circularity(s).getRealDouble());
        pFuncAdder.accept("roundness", s -> opService.geom().roundness(s).getRealDouble());
        pFuncAdder.accept("convexity", s -> opService.geom().convexity(s).getRealDouble());
        pFuncAdder.accept("size", s -> opService.geom().size(s).getRealDouble());
        pFuncAdder.accept("sizeConvexHull", s -> opService.geom().sizeConvexHull(s).getRealDouble());
        pFuncAdder.accept("majorAxis", s -> opService.geom().majorAxis(s).getRealDouble());
        pFuncAdder.accept("minorAxis", s -> opService.geom().minorAxis(s).getRealDouble());
        pFuncAdder.accept("mainElongation", s -> opService.geom().mainElongation(s).getRealDouble());

        iiMaskFuncAdder.accept("sizeMask", s -> opService.geom().size(s).getRealDouble());

        if(!all & (featuresToCompute.size() > 0))
            throw new AssertionError("Not all features in the list were recognized.");

        logService.info("Computing " + featCounter + " features per channel.");
    }

    public static class FeatureVector {
        private final Map<String, Object> map = new HashMap<>();

        public void add(String key, Long channel, double value) {
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

        public <U> void computeFeature(
                String key,
                Long channel,
                Function<U, Double> func,
                U slice,
                boolean compute) {
            if(compute) {
                add(key, channel, func.apply(slice));
            } else {
                add(key, channel, NaN);
            }
        }
    }

    private void computeOnMask(FeatureVector vec, Image<T> image, boolean compute, int pos, long channel) {

        Polygon2D polygon = opService.geom().contour(opService.transform().hyperSliceView(image.getMaskImg(), 2, pos), false);
        for (Map.Entry<String, Function<Polygon2D, Double>> entry : pFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), polygon, compute);
        }

        IterableInterval<NativeBoolType> iiMask = Views.hyperSlice(image.getMaskImg(), 2, pos);
        for(Map.Entry<String, Function<IterableInterval<NativeBoolType>, Double>> entry : iiMaskFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), iiMask, compute);
        }
    }

    private void compute(FeatureVector vec, IntervalView<T> iv, boolean compute, long channel) {

        for(Map.Entry<String, Function<Iterable<T>, Double>> entry : iFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), iv, compute);
        }
        for(Map.Entry<String, Function<IterableInterval<T>, Double>> entry : iiFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), iv, compute);
        }

        for(Map.Entry<String, Function<RandomAccessibleInterval<T>, Double>> entry : raiFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), iv, compute);
        }

        ImageProcessor ip = ImageJFunctions.wrap(iv, "image").getProcessor();
        for(Map.Entry<String, Function<ImageProcessor, Double>> entry : ipFeatureFunctions.entrySet()) {
            vec.computeFeature(entry.getKey(), channel, entry.getValue(), ip, compute);
        }

    }

    public FeatureVector computeVector(Image<T> img, boolean masked) {

        FeatureVector vec = new FeatureVector();
        vec.add("file", img.getFilename());
        vec.add("directory", img.getDirectory());
        vec.add("id", img.getId());

        ImgFactory<T> factory = img.getFactory();

        boolean[] compute = new boolean[img.getChannels().size()];
        Img<T> output;
        if (masked) {
            output = factory.create(img.getMaskImg().dimensionsAsLongArray());
            Cursor<NativeBoolType> cursor = img.getMaskImg().localizingCursor();
            RandomAccess<T> outputRandomAccess = output.randomAccess();
            RandomAccess<T> imgRandomAccess = img.getImg().randomAccess();
            while(cursor.hasNext()) {
                cursor.fwd();
                outputRandomAccess.setPosition(cursor);
                imgRandomAccess.setPosition(cursor);
                if(cursor.get().get()) {
                    T type = imgRandomAccess.get();
                    outputRandomAccess.get().set(type);
                    compute[cursor.getIntPosition(2)] = true;
                } else {
                    outputRandomAccess.get().setZero();
                }
            }
        } else {
            output = img.getImg();
            Arrays.fill(compute, true);
        }

        for (int i = 0; i<img.getChannels().size(); i++) {

            Long channel = img.getChannels().get(i);

            if (masked) {
                computeOnMask(vec, img, compute[i], i, channel);
            }

            IntervalView<T> iv = opService.transform().hyperSliceView(output, 2, i);
            compute(vec, iv, compute[i], channel);
        }
        return vec;
    }
}
