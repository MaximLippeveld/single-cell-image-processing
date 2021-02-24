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
package be.maximl.data.loaders;

import be.maximl.data.Image;
import io.scif.FormatException;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.filters.ReaderFilter;
import io.scif.img.IO;
import io.scif.img.ImageRegion;
import io.scif.img.Range;
import io.scif.img.SCIFIOImgPlus;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public abstract class Loader<T extends NativeType<T> & RealType<T>> implements Iterator<Image<T>> {

    /**
     * Wrapper that prevents SCIFIO's ImgOpener.openImgs method from closing the reader after reading in an image.
     * Closing the reader is handled by the us.
     */
    public static class CloseNoOpReader extends ReaderFilter {

        /**
         * @param r - Reader to be wrapped
         */
        public CloseNoOpReader(Reader r) {
            super(r);
        }

        @Override
        public void close() {
        }
    }

    protected Iterator<File> lister;
    final protected List<Long> channels;
    final protected int imageLimit;
    protected final LogService log;
    protected Reader currentReader;
    protected Iterator<Img<T>> iterator;
    protected int currentIndex = 0;
    protected int currentFinalIndex = 0;
    final protected SCIFIO scifio;

    protected Loader(Iterator<File> lister, List<Long> channels, int imageLimit, LogService log, SCIFIO scifio) {
        this.lister = lister;
        this.channels = channels;
        this.imageLimit = imageLimit;
        this.log = log;
        this.scifio = scifio;

        try {
            currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
            currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
        } catch (IOException | FormatException e) {
            log.error(e);
        }
        iterator = initializeNewIterator();
    }

    abstract protected Iterator<Img<T>> initializeNewIterator();

    protected T getType() {
        Img<T> img = (Img<T>) IO.open(new Loader.CloseNoOpReader(currentReader), new SCIFIOConfig()).getImg();
        return img.firstElement().createVariable();
    }

    protected  <U extends RealType<U>> Iterator<Img<U>> getIterator(Iterator<Integer> indices, ImgFactory<U> factory, Reader reader) {

        SCIFIOConfig config = new SCIFIOConfig();
        config.imgOpenerSetOpenAllImages(false);
        config.imgOpenerSetRegion(
                new ImageRegion(new AxisType[]{Axes.CHANNEL}, new Range(channels.stream().mapToLong(l -> l).toArray())));

        Loader.CloseNoOpReader noOpreader = new Loader.CloseNoOpReader(reader);

        return new Iterator<Img<U>>() {
            @Override
            public boolean hasNext() {
                return indices.hasNext();
            }

            @Override
            public SCIFIOImgPlus<U> next() {
                int imgIndex = indices.next();
                config.imgOpenerSetIndex(imgIndex);
                return IO.open(noOpreader, factory.type(), factory, config);
            }
        };
    }

    protected Image<T> createImage(Reader reader, int id, Img<T> planes) {
        Image<T> image = new Image<>(id);
        image.setDirectory(reader.getMetadata().getSourceLocation().getURI().getPath());
        image.setFilename(reader.getMetadata().getSourceLocation().getName());
        image.setExtension(reader.getFormatName());
        image.setChannels(channels);
        image.setAxesLengths(planes.dimensionsAsLongArray());

        image.setPlanes(planes);

        return image;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Image<T> next() {

        try {
            Img<T> planes = iterator.next();
            Image<T> image = createImage(currentReader, currentIndex, planes);
            currentIndex++;

            if (!iterator.hasNext() & lister.hasNext()) {
                // close current reader
                currentReader.close();

                // initialize new reader
                currentReader = scifio.initializer().initializeReader(new FileLocation(lister.next()));
                currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
                iterator = initializeNewIterator();
            }

            return image;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            log.error("Format exception " + currentReader.getMetadata().getDatasetName());
            e.printStackTrace();
        }

        return null;
    }

    public boolean isMasked() {
        return false;
    }
}
