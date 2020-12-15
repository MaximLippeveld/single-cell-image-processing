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

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.roi.MaskInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.Serializable;
import java.util.List;

public interface Image<T extends RealType<T>, S extends NativeType<S>> extends Serializable {

    int getId();

    short[] getPlanes();

    void setPlanes(short[] planes);

    void setMasks(boolean[] masks);

    void setPlaneLengths(long[] planeLengths);

    RandomAccessibleInterval<T> getImg();

    ImgFactory<T> getFactory();

    MaskInterval getMaskInterval(int pos);

    IterableInterval<S> getMaskAsIterableInterval(int pos);

    String getDirectory();

    String getExtension();

    String getFilename();

    long getSize();

    List<Integer> getChannels();

    void setChannels(List<Integer> channels);

    void setDirectory(String directory);

    void setExtension(String extension);

    void setFilename(String filename);

    void setSize(long size);

    boolean[] getMaskAsBooleanArray(int i);

    long[] getDimensions();
}
