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
package be.maximl.app;

import be.maximl.data.Image;
import be.maximl.data.loaders.Loader;
import be.maximl.data.loaders.imp.CIFLoader;
import be.maximl.data.RecursiveExtensionFilteredLister;
import be.maximl.data.validators.ConnectedComponentsValidator;
import io.scif.FormatException;
import io.scif.Reader;
import io.scif.config.SCIFIOConfig;
import io.scif.img.IO;
import io.scif.img.ImageRegion;
import io.scif.img.Range;
import io.scif.img.SCIFIOImgPlus;
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
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.io.location.FileLocation;
import org.scijava.util.Colors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageViewerApp {

    public static void main(String[] args) throws IOException, FormatException {

        String importDirectory = ImageViewerApp.class.getClassLoader().getResource("image.tiff").getPath();
        String maskImportDirectory = ImageViewerApp.class.getClassLoader().getResource("mask.tiff").getPath();

        ImageJ ij = new ImageJ();

        Reader reader = ij.scifio().initializer().initializeReader(new FileLocation(importDirectory));
        Reader maskReader = ij.scifio().initializer().initializeReader(new FileLocation(maskImportDirectory));

        SCIFIOImgPlus<?> im = IO.open(importDirectory);

        SCIFIOConfig config = new SCIFIOConfig();
//        config.imgOpenerSetIndex(0);
//        ImageRegion region = new ImageRegion(
//                new AxisType[]{im.axis(2).type()}, new Range(new long[]{1,2})
//        );
//        config.imgOpenerSetRegion(region);

        SCIFIOImgPlus<FloatType> tiffImage = IO.open(importDirectory, new ArrayImgFactory<>(new FloatType()), config);
        SCIFIOImgPlus<NativeBoolType> mask = IO.open(maskImportDirectory, new ArrayImgFactory<>(new NativeBoolType()), config);

        Img<FloatType> output = tiffImage.factory().create(tiffImage.dimensionsAsLongArray());
        RandomAccess<FloatType> outputracc = output.randomAccess();
        RandomAccess<FloatType> tiffracc = tiffImage.randomAccess();
        Cursor<NativeBoolType> maskCursor = mask.localizingCursor();
        long[] pos = new long[3];
        while (maskCursor.hasNext()) {
            maskCursor.fwd();
            maskCursor.localize(pos);
            outputracc.setPosition(pos);
            tiffracc.setPosition(pos);
            if (maskCursor.get().get()) {
                outputracc.get().set(tiffracc.get());
            } else {
                outputracc.get().setZero();
            }
        }
        ij.ui().show(output);
        ij.ui().show(mask);


        // read the data
//        final long startTime = System.currentTimeMillis();

//        RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
//        lister.setFileLimit(5);
//        lister.setPath(importDirectory);
//        lister.addExtension("cif");

//        ConnectedComponentsValidator<UnsignedShortType> validator = new ConnectedComponentsValidator<>(opService);
//        Loader<UnsignedShortType> relation = new CIFLoader<>(ij.log(), -1, Arrays.asList(0L, 5L, 8L), lister.getFiles().iterator(), ij.scifio(), validator, new UnsignedShortType());
//
////        relation.setStartIndex(17111);
//
//        Image<UnsignedShortType> img = relation.next();
//
//        ImgFactory<NativeBoolType> factory = new ArrayImgFactory<>(new NativeBoolType());
//        Img<NativeBoolType> maskImg = factory.create(img.getDimensions()[0], img.getDimensions()[1]);
//        Cursor<NativeBoolType> maskCursor = img.getMaskAsIterableInterval(0).localizingCursor();
//        RandomAccess<NativeBoolType> racc = maskImg.randomAccess();
//        long[] pos = new long[2];
//        while (maskCursor.hasNext()) {
//            maskCursor.fwd();
//            maskCursor.localize(pos);
//            racc.setPosition(pos);
//            racc.get().set(maskCursor.get());
//        }
//        ij.ui().show(maskImg);
//
//        IterableInterval<UnsignedShortType> it = Regions.sample(img.getMaskInterval(0), img.getImg(0));
//        Img<UnsignedShortType> it2img = ij.op().create().img(it);
//        RandomAccess<UnsignedShortType> racc2 = it2img.randomAccess();
//
//        int counter = 0;
//        int sum = 0;
//        Cursor<UnsignedShortType> cursor = it.localizingCursor();
//        while (cursor.hasNext()) {
//            cursor.fwd();
//            UnsignedShortType t = cursor.get();
//            if(t.get() > 0) {
//                counter++;
//                sum += t.get();
//                racc2.setPosition(cursor);
//                racc2.get().set(t.get());
//            }
//        }
//        ij.log().info("COUNT " + counter + " SUM " + sum);
//        cursor = it2img.cursor();
//        counter = 0;
//        sum = 0;
//        while (cursor.hasNext()) {
//            cursor.fwd();
//            UnsignedShortType t = cursor.get();
//            if(t.get() > 0) {
//                counter++;
//                sum += t.get();
//            }
//        }
//        ij.log().info("COUNT " + counter + " SUM " + sum);
//
//        ij.ui().show(it2img);

//        ImageDisplay id = (ImageDisplay) ij.display().createDisplay(img.getImg(0));

//        Polygon2D polygon = ij.op().geom().contour(Masks.toRandomAccessibleInterval(img.getMaskInterval(0)), true);
//        ij.log().info(ij.op().geom().size(polygon));
//
//        IterableInterval<NativeBoolType> itMask = img.getMaskAsIterableInterval(0);
//        ij.log().info(ij.op().geom().size(itMask));
//
//        List<Overlay> overlays = new ArrayList<>();
//        List<RealLocalizable> vertices = polygon.vertices();
//        double[] posA = vertices.get(0).positionAsDoubleArray();
//        double[] posB;
//        for(int i = 1; i<vertices.size(); i++) {
//            posB = vertices.get(i).positionAsDoubleArray();
//            LineOverlay line = new LineOverlay(ij.getContext());
//            line.setAlpha(200);
//            line.setFillColor(Colors.CHOCOLATE);
//            line.setLineWidth(1);
//            line.setLineStart(posA);
//            line.setLineEnd(posB);
//            overlays.add(line);
//
//            ij.log().info("line from " + Arrays.toString(posA) + " to " + Arrays.toString(posB));
//
//            posA = posB.clone();
//        }
//
//        ij.overlay().addOverlays(id, overlays);
//
//        RandomAccessibleInterval<UnsignedShortType> sobel = ij.op().filter().sobel(it2img);
//        ij.ui().show(sobel);
//
//        sum = 0;
//        counter = 0;
//        for ( UnsignedShortType t : ImgView.wrap(sobel)) {
//            sum += Math.pow(t.getRealDouble(), 2);
//            counter++;
//        }
//        ij.log().info(Math.sqrt((double)sum/counter));
//
//        ij.ui().show(Masks.toRandomAccessibleInterval(img.getMaskInterval(0)));
//        ij.ui().show(ij.op().create().img(it));
//        ij.ui().show(ij.op().create().img(ImgView.wrap(sobel)));
//        ij.ui().show(id);

    }

}
