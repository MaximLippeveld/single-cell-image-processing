package be.maximl.app;

import be.maximl.data.Image;
import be.maximl.data.RecursiveExtensionFilteredLister;
import be.maximl.data.loaders.Loader;
import be.maximl.data.loaders.imp.TIFFLoader;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;
import io.scif.SCIFIO;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

@Plugin(type = Command.class, menuPath = "Plugins>Single-cell imaging>Mask")
public class MaskApp<T extends RealType<T> & NativeType<T>> implements Command {

    @Parameter
    private OpService ops;

    @Parameter
    private LogService log;

    @Parameter
    private SCIFIO scifio;

    @Parameter
    private UIService uiService;


    public static void main(String[] args) {

        final ImageJ ij = new ImageJ();

        Future<CommandModule> command = ij.command().run(MaskApp.class, true, Collections.EMPTY_MAP);
        ij.module().waitFor(command);
        ij.getContext().dispose();
    }


    @Override
    public void run() {

        String importDir = "/data/Experiment_data/VIB/Vulcan/Slava_PBMC/images_subset";

        RecursiveExtensionFilteredLister lister = new RecursiveExtensionFilteredLister();
        lister.setPath(importDir);
        lister.addExtension("tiff");

        List<Long> channels = new ArrayList<>();
        channels.add(0L);

        Loader<T> loader = new TIFFLoader<>(lister.getFiles().iterator(), channels, log, scifio);
        Image<T> image = loader.next();

        RandomAccessibleInterval<T> rai = ops.transform().hyperSliceView(image.getImg(), 2, 0);
        RandomAccessibleInterval<T> smooth = ops.filter().gauss(rai, 1);
        IterableInterval<T> smoothIterable = ops.transform().flatIterableView(smooth);

        T thresh = ops.threshold().huang(ops.image().histogram(smoothIterable));
        IterableInterval<BitType> mask = ops.threshold().apply(smoothIterable, thresh);
        RandomAccessibleInterval<BitType> filledMask = ops.morphology().fillHoles(ops.create().img(mask));

        uiService.show(ops.create().img(filledMask));

        ImagePlus imagePlus = ImageJFunctions.wrap(filledMask, "mask");

        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.AREA, rt, 10, 100000);
        particleAnalyzer.analyze(imagePlus);

        ImagePlus filteredMask = particleAnalyzer.getOutputImage();

//        uiService.show(ImageJFunctions.wrap(filteredMask));
    }
}
