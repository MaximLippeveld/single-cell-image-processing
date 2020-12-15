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
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.BooleanArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.List;

/**
 * A good old JavaBean containing the EXIF properties as well as the
 * SparkColumn annotation.
 * 
 * @author jgp
 */
public class BioFormatsImage implements Image<UnsignedShortType, NativeBoolType> {
  private static final long serialVersionUID = -2589804417011601051L;

  final private static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Integer> channels;
  private short[] planes;
  private ArrayImg<UnsignedShortType, ShortArray> img;
  private ArrayImg<NativeBoolType, BooleanArray> maskImg;
  private boolean[] masks;
  final private long[] dims = new long[3];
  final private int id;

  public BioFormatsImage(int id) {
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public short[] getPlanes() {
    return planes;
  }

  @Override
  public void setPlanes(short[] planes) {
    this.planes = planes;
    img =  ArrayImgs.unsignedShorts(planes, getDimensions());
  }

  @Override
  public void setMasks(boolean[] masks) {
    this.masks = masks;
    maskImg = ArrayImgs.booleans(masks, getDimensions());
  }

  @Override
  public void setPlaneLengths(long[] planeLengths) {
    dims[0] = planeLengths[0];
    dims[1] = planeLengths[1];
  }

  @Override
  public long[] getDimensions() {
    return dims;
  }

  @Override
  public RandomAccessibleInterval<UnsignedShortType> getImg() {
    return img;
  }

  private ArrayImg<NativeBoolType, BooleanArray> getMaskImg() {
    return maskImg;
  }

  @Override
  public ImgFactory<UnsignedShortType> getFactory() {
    return new ArrayImgFactory<>(new UnsignedShortType());
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
  public boolean[] getMaskAsBooleanArray(int i) {
    int planeSize = (int) (getDimensions()[0] * getDimensions()[1]);
    return Arrays.copyOfRange(masks, i*planeSize, (i+1)*planeSize);
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

  /**
   * @return the size
   */
  @Override
  public long getSize() {
    return size;
  }

  @Override
  public List<Integer> getChannels() {
    return channels;
  }

  @Override
  public void setChannels(List<Integer> channels) {
    this.channels = channels;
    dims[2] = channels.size();
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


  /**
   * @param size
   *          the size to set
   */
  @Override
  public void setSize(long size) {
    this.size = size;
  }

}
