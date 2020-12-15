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
package be.maximl.data.bf;

import be.maximl.data.*;
import be.maximl.data.validators.Validator;
import io.scif.FormatException;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.io.location.FileLocation;
import org.scijava.log.LogService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;


public class BioFormatsLoader implements Loader<UnsignedShortType, NativeBoolType> {
  private static final long serialVersionUID = 4598175080399877334L;
  private final LogService log;
  private Iterator<File> lister;
  final private ArrayList<Integer> channels = new ArrayList<>();
  final private SCIFIO scifio = new SCIFIO();
  final private Validator<UnsignedShortType, NativeBoolType> validator;
  private int imageLimit = -1;

  private Reader currentReader;
  private int currentFinalIndex;
  private int currentIndex = 0;

  public BioFormatsLoader(LogService log, Validator<UnsignedShortType, NativeBoolType> validator) {
    this.log = log;
    this.validator = validator;
  }

  @Override
  public Image<UnsignedShortType, NativeBoolType> imageFromReader(Reader reader, int index) throws IOException, FormatException {

    int imgIndex;
    int maskIndex;
    int pointsInPlane;
    Plane imgPlane;
    Plane maskPlane;
    short[] flatData;
    boolean[] maskData;
    short[] planeTmp;
    byte[] maskTmp;
    Image<UnsignedShortType, NativeBoolType> image;

    imgIndex = index;
    maskIndex = index+1;

    image = new BioFormatsImage(imgIndex/2);
    image.setDirectory(reader.getMetadata().getSourceLocation().getURI().getPath());
    image.setFilename(reader.getMetadata().getSourceLocation().getName());
    image.setExtension(reader.getFormatName());
    image.setChannels(channels);

    imgPlane = reader.openPlane(imgIndex, channels.get(0));
    image.setPlaneLengths(imgPlane.getLengths());
    image.setSize(imgPlane.getBytes().length * channels.size());

    pointsInPlane = imgPlane.getBytes().length / 2;

    flatData = new short[channels.size()*pointsInPlane];
    planeTmp = new short[pointsInPlane];
    maskData = new boolean[channels.size()*pointsInPlane];

    for (int j = 0; j<channels.size(); j++) {

      maskPlane = reader.openPlane(maskIndex, channels.get(j));
      for (int k = 0; k<pointsInPlane; k++) {
        maskData[k+j*pointsInPlane] = maskPlane.getBytes()[k] > 0;
      }

      if (j > 0) {
        // the plane at j=0 was already loaded in
        imgPlane = reader.openPlane(imgIndex, channels.get(j));
      }
      ByteBuffer.wrap(imgPlane.getBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(planeTmp);
      System.arraycopy(planeTmp, 0, flatData, j * pointsInPlane, pointsInPlane);
    }

    image.setPlanes(flatData);
    image.setMasks(maskData);

    return image;
  }

  @Override
  public void addChannel(int channel) {
    this.channels.add(channel);
  }

  @Override
  public void setImageLimit(int imageLimit) {
    this.imageLimit = imageLimit;
  }

  @Override
  public void setStartIndex(int index) {
    this.currentIndex = index*2;
  }

  @Override
  public void setLister(
          FileLister lister) throws IOException, FormatException {
    this.lister = lister.getFiles().iterator();
    currentReader = scifio.initializer().initializeReader(new FileLocation(this.lister.next()));
    currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
  }

  @Override
  public boolean hasNext() {
    return currentIndex < currentFinalIndex;
  }

  @Override
  public Image<UnsignedShortType, NativeBoolType> next() {

    try {
      Image<UnsignedShortType, NativeBoolType> image = imageFromReader(currentReader, currentIndex);
      currentIndex += 2;

      if (currentIndex >= currentFinalIndex) {
        if (lister.hasNext()) {
          currentIndex = 0;

          // initialize new reader
          FileLocation loc = new FileLocation(lister.next());
          currentReader = scifio.initializer().initializeReader(loc);
          currentFinalIndex = imageLimit == -1 ? currentReader.getImageCount() : imageLimit;
        }
      }

      boolean valid = validator.validate(image);
      if (!valid)
        return null;

      return image;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      log.error("Format exception " + currentReader.getMetadata().getDatasetName());
      e.printStackTrace();
    }

    return null;
  }

}
