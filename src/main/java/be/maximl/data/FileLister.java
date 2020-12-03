package be.maximl.data;

import java.io.File;
import java.io.Serializable;
import java.util.List;

public interface FileLister extends Serializable {
    List<File> getFiles();
}
