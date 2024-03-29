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

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.RealType;

import java.util.List;

public class Image<T extends NativeType<T> & RealType<T>> {

  final public static int CHANNELDIM = 2;

  private String directory;
  private String extension;
  private String filename;
  private long size;
  private List<Long> channels;
  private Img<T> img;
  private Img<NativeBoolType> maskImg;
  private long[] dims;
  final private int id;

  public Image(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void setPlanes(Img<T> planes) {
    img = planes;
  }

  public void setMasks(Img<NativeBoolType> masks) {
    maskImg = masks;
  }

  public void setAxesLengths(long[] planeLengths) {
    this.dims = planeLengths;
  }

  public long[] getDimensions() {
    return dims;
  }

  public Img<T> getImg() {
    return img;
  }

  public Img<NativeBoolType> getMaskImg() {
    return maskImg;
  }

  public ImgFactory<T> getFactory() {
    return img.factory();
  }

  /**
   * @return the directory
   */
  public String getDirectory() {
    return directory;
  }

  /**
   * @return the extension
   */
  public String getExtension() {
    return extension;
  }


  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }


  public List<Long> getChannels() {
    return channels;
  }

  public void setChannels(List<Long> channels) {
    this.channels = channels;
  }


  /**
   * @param directory
   *          the directory to set
   */
  public void setDirectory(String directory) {
    this.directory = directory;
  }

  /**
   * @param extension
   *          the extension to set
   */
  public void setExtension(String extension) {
    this.extension = extension;
  }


  /**
   * @param filename
   *          the filename to set
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

}
