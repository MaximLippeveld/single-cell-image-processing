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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lists the files based on the criteria we have defined like: recursivity,
 * maximum number of files,
 * 
 * @author jgp
 */
public class RecursiveExtensionFilteredLister implements FileLister {
  @SuppressWarnings("unused")
  private static final long serialVersionUID = -5751014237854623589L;

  private List<String> extensions;
  private List<File> files;
  private int fileLimit;
  private boolean recursive;
  private boolean hasChanged;
  private String startPath = null;

  /**
   * Constructor, sets the default values.
   */
  public RecursiveExtensionFilteredLister() {
    this.files = new ArrayList<>();
    this.extensions = new ArrayList<>();
    this.recursive = false;
    this.fileLimit = -1;
    this.hasChanged = true;
  }

  /**
   * Adds an extension to filter. Can start with a dot or not. Is case
   * sensitive: .jpg is not the same as .JPG.
   * 
   * @param extension
   *          to filter on.
   */
  public void addExtension(String extension) {
    if (extension.startsWith(".")) {
      this.extensions.add(extension);
    } else {
      this.extensions.add("." + extension);
    }
    this.hasChanged = true;
  }

  /**
   * Checks if the file is matching our constraints.
   * 
   * @param dir
   * @param name
   * @return
   */
  private boolean check(File dir, String name) {
    File f = new File(dir, name);
    if (f.isDirectory()) {
      if (recursive) {
        list0(f);
      }
      return false;
    } else {
      for (String ext : extensions) {
        if (name.toLowerCase().endsWith(ext)) {
          return true;
        }
      }
      return false;
    }
  }

  private boolean dir() {
    if (this.startPath == null) {
      return false;
    }
    return list0(new File(this.startPath));
  }

  /**
   * Returns a list of files based on the criteria given by setters (or
   * default values)
   * 
   * @return a list of files
   */
  @Override
  public List<File> getFiles() {
    if (this.hasChanged == true) {
      dir();
      this.hasChanged = false;
    }
    return files;
  }

  private boolean list0(File folder) {
    if (folder == null) {
      return false;
    }
    if (!folder.isDirectory()) {
      return false;
    }

    File[] listOfFiles = folder.listFiles((dir, name) -> check(dir, name));
    if (listOfFiles == null) {
      return true;
    }
    if (fileLimit == -1) {
      this.files.addAll(Arrays.asList(listOfFiles));
    } else {
      int fileCount = this.files.size();
      if (fileCount >= fileLimit) {
        recursive = false;
        return false;
      }

      for (int i = fileCount, j = 0; i < fileLimit
          && j < listOfFiles.length; i++, j++) {
        this.files.add(listOfFiles[j]);
      }
    }
    return true;
  }

  /**
   * Sets the limit of files you want to list. Default is no limit.
   * 
   * @param i
   */
  public void setFileLimit(int i) {
    this.fileLimit = i;
    this.hasChanged = true;
  }

  /**
   * Sets the starting path.
   * 
   * @param newPath
   */
  public void setPath(String newPath) {
    this.startPath = newPath;
    this.hasChanged = true;
  }

  /**
   * Sets the recursion, default is false.
   * 
   * @param b
   */
  public void setRecursive(boolean b) {
    this.recursive = b;
    this.hasChanged = true;
  }

}
