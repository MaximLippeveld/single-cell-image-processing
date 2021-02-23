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

import be.maximl.data.*;
import be.maximl.data.validators.Validator;
import io.scif.*;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Expands input files containing many images into processable images.
 *
 * @param <T> Image data type
 */
public abstract class MaskedLoader<T extends NativeType<T> & RealType<T>> extends Loader<T> {

  final private Validator<T> validator;
  final protected Iterator<File> maskLister;
  private Iterator<Img<NativeBoolType>> maskIterator;
  protected Reader maskReader;

  public MaskedLoader(LogService log, int imageLimit, List<Long> channels, Iterator<File> lister, Iterator<File> maskLister, SCIFIO scifio, Validator<T> validator) {
    super(lister, channels, imageLimit, log, scifio);
    this.validator = validator;
    this.maskLister = maskLister;

    try {
      maskReader = scifio.initializer().initializeReader(new FileLocation(maskLister.next()));
    } catch (IOException | FormatException e) {
      log.error(e);
    }
    maskIterator = initializeNewMaskIterator();
  }

  abstract protected Iterator<Img<NativeBoolType>> initializeNewMaskIterator();

  @Override
  public Image<T> next() {

      try {
          Image<T> image = super.next();

          Img<NativeBoolType> mask = maskIterator.next();
          image.setMasks(mask);

          boolean valid = validator.validate(image);
          if (!valid)
            return null;

          if (!maskIterator.hasNext() & maskLister.hasNext()) {
            // close current reader
            maskReader.close();

            // initialize new reader
            maskReader = scifio.initializer().initializeReader(new FileLocation(maskLister.next()));
            maskIterator = initializeNewMaskIterator();
          }

          return image;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (FormatException e) {
        log.error("Format exception " + maskReader.getMetadata().getDatasetName());
        e.printStackTrace();
      }
      return null;
  }

  @Override
  public boolean isMasked(){
    return true;
  }

}
