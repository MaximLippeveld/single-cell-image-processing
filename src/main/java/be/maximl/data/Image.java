/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 Maxim Lippeveld
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
package be.maximl.data;

import ij.process.ImageProcessor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.roi.MaskInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.IntervalView;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper class which holds image-data to be processed.
 *
 * @param <T>
 */
public interface Image<T extends NativeType<T>> extends Serializable {

    /**
     * @return unique identifier
     */
    int getId();

    void setPlanes(Img<T> planes);

    void setMasks(Img<NativeBoolType> masks);

    void setAxesLengths (long[] axesLengths);

    RandomAccessibleInterval<T> getRAI();

    ImgFactory<T> getFactory();

    MaskInterval getMaskInterval(int pos);

    IterableInterval<NativeBoolType> getMaskAsIterableInterval(int pos);

    String getDirectory();

    String getExtension();

    String getFilename();

    List<Long> getChannels();

    void setChannels(List<Long> channels);

    void setDirectory(String directory);

    void setExtension(String extension);

    void setFilename(String filename);

    ImageProcessor getMaskAsImageProcessor(int i);

    long[] getDimensions();
}
