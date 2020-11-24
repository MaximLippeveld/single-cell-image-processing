package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import net.imagej.ImageJ;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.util.HashMap;
import java.util.Map;

public class FeatureVectorFactory {

    final private ImageJ ij = new ImageJ();

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

        public Map<String, String> getMap() {
            return map;
        }

        public String[] getLine() {
            return map.values().toArray(new String[0]);
        }
    }

    public FeatureVector computeVector(BioFormatsImage img) {

        FeatureVector vec = new FeatureVector();
        vec.add("file", img.getFilename());
        vec.add("directory", img.getDirectory());

        RandomAccessibleInterval<UnsignedShortType> libImg = img.getImg();

        IterableInterval<UnsignedShortType> slice;
        Integer channel;
        for (int i = 0; i<img.getChannels().size(); i++) {

            // create an iterator that only samples points within the mask
            slice = Regions.sample(img.getMask(i), Views.hyperSlice(libImg, 0, i));

            channel = img.getChannels().get(i);
            vec.add("geometricMean", channel, ij.op().stats().geometricMean(slice).getRealDouble());
            vec.add("harmonicMean", channel, ij.op().stats().harmonicMean(slice).getRealDouble());
            vec.add("stdDev", channel, ij.op().stats().stdDev(slice).getRealDouble());
            vec.add("median", channel, ij.op().stats().median(slice).getRealDouble());
            vec.add("sum", channel, ij.op().stats().sum(slice).getRealDouble());
            vec.add("min", channel, ij.op().stats().min(slice).getRealDouble());
            vec.add("max", channel, ij.op().stats().max(slice).getRealDouble());
            vec.add("kurtosis", channel, ij.op().stats().kurtosis(slice).getRealDouble());
            vec.add("skewness", channel, ij.op().stats().skewness(slice).getRealDouble());
            vec.add("moment3AboutMean", channel, ij.op().stats().moment3AboutMean(slice).getRealDouble());

//            ZernikeNamespace zernike = ij.op().zernike();
//            vec.add("zernikeMagnitude", channel, zernike.magnitude(slice, 0, 1).getRealDouble());
//            vec.add("zernikePhase", channel, zernike.phase(slice, 0, 1).getRealDouble());
//
//            HaralickNamespace haralick = ij.op().haralick();
        }

        return vec;
    }

}
