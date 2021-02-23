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

import be.maximl.data.loaders.MaskedLoader;
import be.maximl.data.validators.Validator;
import io.scif.SCIFIO;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import org.scijava.log.LogService;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class CIFLoader<T extends NativeType<T> & RealType<T>> extends MaskedLoader<T> {

    public CIFLoader(LogService log, int imageLimit, List<Long> channels, Iterator<File> lister, SCIFIO scifio, Validator<T> validator) {
        super(log, imageLimit, channels, lister, lister, scifio, validator);
    }

    @Override
    protected Iterator<Img<NativeBoolType>> initializeNewMaskIterator() {
        ImgFactory<NativeBoolType> factory = new ArrayImgFactory<>(new NativeBoolType());
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 != 0).iterator(), factory, currentReader);
    }

    @Override
    protected Iterator<Img<T>> initializeNewIterator() {
        ImgFactory<T> factory = new ArrayImgFactory<>(getType());
        return getIterator(IntStream.range(currentIndex, currentFinalIndex).filter(l -> l%2 == 0).iterator(), factory, currentReader);
    }
}
