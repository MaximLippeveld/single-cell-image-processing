package be.maximl.app;

import io.scif.FormatException;
import io.scif.Plane;
import io.scif.SCIFIO;
import io.scif.filters.ReaderFilter;
import org.scijava.table.Tables;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import org.scijava.io.location.FileLocation;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.img.array.ArrayImg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

public class LoadImage {
    public static void main (final String... args) throws FormatException, IOException {

        SCIFIO scifio = new SCIFIO();

        FileLocation loc = new FileLocation("/home/maximl/Data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/C1_T0_49.cif");

        ReaderFilter reader = scifio.initializer().initializeReader(loc);

        int n = reader.getImageCount();
        n = 100;
        int c = (int) reader.getPlaneCount(0);

        int imgIndex;
        int maskIndex;
        HashMap<String, float[][]> resultsMap = new HashMap<>();
        resultsMap.put("geometricMean", new float[n][c]);

        for (int i=0; i<n; i+=2) {
            imgIndex = i;
            maskIndex = i+1;
            for (int j=0; j<reader.getPlaneCount(i); j++) {
                Plane maskPlane = reader.openPlane(maskIndex, j);

                Plane imgPlane = reader.openPlane(imgIndex, j);
                short[] imgShorts = new short[imgPlane.getBytes().length/2];
                ByteBuffer.wrap(imgPlane.getBytes()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(imgShorts);

                ArrayImg<UnsignedShortType, ShortArray> img = ArrayImgs.unsignedShorts(imgShorts, imgPlane.getLengths());

            }
        }
    }
}
