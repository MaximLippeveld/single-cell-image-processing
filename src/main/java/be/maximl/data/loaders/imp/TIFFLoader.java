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
package be.maximl.data.loaders.imp;

import be.maximl.data.loaders.Loader;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.log.LogService;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TIFFLoader<T extends NativeType<T> & RealType<T>> extends Loader<T> {

    public TIFFLoader(Iterator<File> lister, List<Long> channels, LogService log, SCIFIO scifio) {
        super(lister, channels, 1, log, scifio);
    }

    @Override
    protected Iterator<Img<T>> initializeNewIterator() {
        ImgFactory<T> factory = new ArrayImgFactory<>(getType());
        return getIterator(Collections.singleton(0).iterator(), factory);
    }
}
