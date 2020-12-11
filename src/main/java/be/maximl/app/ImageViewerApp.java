package be.maximl.app;

import be.maximl.data.Image;
import be.maximl.data.Loader;
import be.maximl.data.bf.BioFormatsLoader;
import be.maximl.data.RecursiveExtensionFilteredLister;
import be.maximl.data.validators.ConnectedComponentsValidator;
import io.scif.FormatException;
import io.scif.config.SCIFIOConfig;
import io.scif.filters.ReaderFilter;
import io.scif.img.IO;
import io.scif.img.ImageRegion;
import io.scif.img.Range;
import io.scif.img.SCIFIOImgPlus;
import io.scif.img.cell.SCIFIOCellImgFactory;
import jdk.jfr.Unsigned;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.overlay.*;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.scijava.io.location.FileLocation;
import org.scijava.util.Colors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageViewerApp {

    public static void main(String[] args) throws IOException, FormatException {

        ImageJ ij = new ImageJ();

        // Import directory
//        String importDirectory = "/data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/C3_T5_69.cif";
        String importDirectory = "/data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/";

        // read the data
        final long startTime = System.currentTimeMillis();

        RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
        lister.setFileLimit(5);
        lister.setPath(importDirectory);
        lister.addExtension("cif");

        ConnectedComponentsValidator<UnsignedShortType> validator = new ConnectedComponentsValidator<>();
        Loader<UnsignedShortType> relation = new BioFormatsLoader<>(ij.log(), Arrays.asList(0L, 5L, 8L), ij.scifio(), validator, new UnsignedShortType());
        relation.setImageLimit(-1);
        try {
            relation.setLister(lister);
        } catch (IOException e) {
            System.err.println("Unknown file");
            e.printStackTrace();
        } catch (FormatException e) {
            System.err.println("Unknown format");
            e.printStackTrace();
        }

//        relation.setStartIndex(17111);

        Image<UnsignedShortType> img = relation.next();

        ImgFactory<NativeBoolType> factory = new ArrayImgFactory<>(new NativeBoolType());
        Img<NativeBoolType> maskImg = factory.create(img.getDimensions()[0], img.getDimensions()[1]);
        Cursor<NativeBoolType> maskCursor = img.getMaskAsIterableInterval(0).localizingCursor();
        RandomAccess<NativeBoolType> racc = maskImg.randomAccess();
        long[] pos = new long[2];
        while (maskCursor.hasNext()) {
            maskCursor.fwd();
            maskCursor.localize(pos);
            racc.setPosition(pos);
            if (maskCursor.get().get()){
                ij.log().info("get");
            }
            racc.get().set(maskCursor.get());
        }
        ij.ui().show(maskImg);

        IterableInterval<UnsignedShortType> it = Regions.sample(img.getMaskInterval(0), Views.hyperSlice(img.getRAI(), 2, 0));
        Img<UnsignedShortType> it2img = ij.op().create().img(it);
        RandomAccess<UnsignedShortType> racc2 = it2img.randomAccess();

        int counter = 0;
        int sum = 0;
        Cursor<UnsignedShortType> cursor = it.localizingCursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            UnsignedShortType t = cursor.get();
            if(t.get() > 0) {
                counter++;
                sum += t.get();
                racc2.setPosition(cursor);
                racc2.get().set(t.get());
            }
        }
        ij.log().info("COUNT " + counter + " SUM " + sum);
        cursor = it2img.cursor();
        counter = 0;
        sum = 0;
        while (cursor.hasNext()) {
            cursor.fwd();
            UnsignedShortType t = cursor.get();
            if(t.get() > 0) {
                counter++;
                sum += t.get();
            }
        }
        ij.log().info("COUNT " + counter + " SUM " + sum);

        ij.ui().show(it2img);

        ImageDisplay id = (ImageDisplay) ij.display().createDisplay(img.getRAI());

        Polygon2D polygon = ij.op().geom().contour(Masks.toRandomAccessibleInterval(img.getMaskInterval(0)), true);
        ij.log().info(ij.op().geom().size(polygon));

        IterableInterval<NativeBoolType> itMask = img.getMaskAsIterableInterval(0);
        ij.log().info(ij.op().geom().size(itMask));

        List<Overlay> overlays = new ArrayList<>();
        List<RealLocalizable> vertices = polygon.vertices();
        double[] posA = vertices.get(0).positionAsDoubleArray();
        double[] posB;
        for(int i = 1; i<vertices.size(); i++) {
            posB = vertices.get(i).positionAsDoubleArray();
            LineOverlay line = new LineOverlay(ij.getContext());
            line.setAlpha(200);
            line.setFillColor(Colors.CHOCOLATE);
            line.setLineWidth(1);
            line.setLineStart(posA);
            line.setLineEnd(posB);
            overlays.add(line);

            ij.log().info("line from " + Arrays.toString(posA) + " to " + Arrays.toString(posB));

            posA = posB.clone();
        }

        ij.overlay().addOverlays(id, overlays);

        RandomAccessibleInterval<UnsignedShortType> sobel = ij.op().filter().sobel(it2img);
        ij.ui().show(sobel);

        sum = 0;
        counter = 0;
        for ( UnsignedShortType t : ImgView.wrap(sobel)) {
            sum += Math.pow(t.getRealDouble(), 2);
            counter++;
        }
        ij.log().info(Math.sqrt((double)sum/counter));

        ij.ui().show(Masks.toRandomAccessibleInterval(img.getMaskInterval(0)));
        ij.ui().show(ij.op().create().img(it));
        ij.ui().show(ij.op().create().img(ImgView.wrap(sobel)));
        ij.ui().show(id);

    }

}
