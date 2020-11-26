package be.maximl.app;

import be.maximl.data.Image;
import be.maximl.data.Loader;
import be.maximl.data.bf.BioFormatsLoader;
import be.maximl.data.RecursiveExtensionFilteredLister;
import io.scif.FormatException;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.scijava.ui.UIService;

import java.io.IOException;

public class ImageViewerApp {

    public static void main(String[] args) {

        ImageJ ij = new ImageJ();

        // Import directory
        String importDirectory = "/home/maximl/Data/Experiment_data/weizmann/EhV/high_time_res/Ctrl/";

        // read the data
        final long startTime = System.currentTimeMillis();

        RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
        lister.setFileLimit(5);
        lister.setPath(importDirectory);
        lister.addExtension("cif");

        Loader relation = new BioFormatsLoader(ij.log());
        for (int i=0;i<3;i++) {
            relation.addChannel(i);
        }
        relation.setImageLimit(-1);
        try {
            relation.setLister(lister);
        } catch (IOException e) {
            System.err.println("Unknown file");
            e.printStackTrace();
        } catch (FormatException e) {
            System.err.println("Unknown format");
            e.printStackTrace();
        }

        Image img = relation.next();
        UIService service = ij.ui();

        service.show(Views.hyperSlice(img.getImg(), 2, 0));
        service.show(Masks.toRandomAccessibleInterval(img.getMask(0)));

        IterableInterval<UnsignedShortType> it = Regions.sample(img.getMask(0), Views.hyperSlice(img.getImg(), 2, 0));

        int counter = 0;
        Cursor<UnsignedShortType> cursor = it.localizingCursor();
        while (cursor.hasNext()) {
            cursor.fwd();
            UnsignedShortType t = cursor.get();
            if(t.get() > 0) {
                counter++;
            }
        }
        ij.log().info("COUNT " + counter);


    }

}
