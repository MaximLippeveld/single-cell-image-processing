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

import be.maximl.data.Image;
import ij.process.ImageProcessor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.List;

/**
 * A good old JavaBean containing the EXIF properties as well as the
 * SparkColumn annotation.
 * 
 * @author jgp
 */
public class BioFormatsImage<T extends NativeType<T> & RealType<T>> implements Image<T> {
  private static final long serialVersionUID = -2589804417011601051L;

  final private static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Long> channels;
  private Img<T> img;
  private Img<NativeBoolType> maskImg;
  private long[] dims;
  final private int id;

  public BioFormatsImage(int id) {
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setPlanes(Img<T> planes) {
    img = planes;
  }

  @Override
  public void setMasks(Img<NativeBoolType> masks) {
    maskImg = masks;
  }

  @Override
  public void setAxesLengths(long[] planeLengths) {
    this.dims = planeLengths;
  }

  @Override
  public long[] getDimensions() {
    return dims;
  }

  @Override
  public RandomAccessibleInterval<T> getRAI() {
    return img;
  }

  private Img<NativeBoolType> getMaskImg() {
    return maskImg;
  }

  @Override
  public ImgFactory<T> getFactory() {
    return img.factory();
  }

  @Override
  public MaskInterval getMaskInterval(int pos) {
    return Masks.toMaskInterval(Views.hyperSlice(getMaskImg(), 2, 0));
  }

  @Override
  public IterableInterval<NativeBoolType> getMaskAsIterableInterval(int pos) {
    return Views.hyperSlice(getMaskImg(), 2, pos);
  }

  @Override
  public ImageProcessor getMaskAsImageProcessor(int i) {
    return ImageJFunctions.wrap(getMaskImg(), "test").getProcessor();
  }

  /**
   * @return the directory
   */
  @Override
  public String getDirectory() {
    return directory;
  }

  /**
   * @return the extension
   */
  @Override
  public String getExtension() {
    return extension;
  }


  /**
   * @return the filename
   */
  @Override
  public String getFilename() {
    return filename;
  }


  @Override
  public List<Long> getChannels() {
    return channels;
  }

  @Override
  public void setChannels(List<Long> channels) {
    this.channels = channels;
  }


  /**
   * @param directory
   *          the directory to set
   */
  @Override
  public void setDirectory(String directory) {
    this.directory = directory;
  }

  /**
   * @param extension
   *          the extension to set
   */
  @Override
  public void setExtension(String extension) {
    this.extension = extension;
  }


  /**
   * @param filename
   *          the filename to set
   */
  @Override
  public void setFilename(String filename) {
    this.filename = filename;
  }

}
