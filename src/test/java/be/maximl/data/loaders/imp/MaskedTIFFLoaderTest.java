package be.maximl.data.loaders.imp;

import be.maximl.data.Image;
import be.maximl.data.validators.ConnectedComponentsValidator;
import be.maximl.data.validators.Validator;
import io.scif.SCIFIO;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.jupiter.api.*;
import org.scijava.log.LogService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MaskedTIFFLoaderTest {

    private static LogService log;
    private static SCIFIO scifio;
    private static OpService opService;

    private MaskedTIFFLoader<FloatType> loader;
    private int expectedNonZero;

    @BeforeAll
    static void setupServices() {
        ImageJ imagej = new ImageJ();
        log = imagej.log();
        scifio = imagej.scifio();
        opService = imagej.op();
    }

    @BeforeEach
    void setupLoader() {
        List<Long> channels = new ArrayList<>();
        channels.add(0L);
        channels.add(1L);
        channels.add(2L);
        channels.add(3L);
        Validator<FloatType> validator = new ConnectedComponentsValidator<>(opService);
        expectedNonZero = 998;

        URL mask = getClass().getClassLoader().getResource("mask.tiff");
        URL image = getClass().getClassLoader().getResource("image.tiff");
        if (mask != null & image != null) {
            File maskFile = new File(mask.getPath());
            File imageFile = new File(image.getPath());

            Iterator<File> maskIterator = Arrays.stream(new File[]{maskFile}).iterator();
            Iterator<File> imageIterator = Arrays.stream(new File[]{imageFile}).iterator();

            loader = new MaskedTIFFLoader<>(
                log, 1, channels, imageIterator, maskIterator, scifio, validator
            );
        }

    }

    @Test
    @DisplayName("Loader should have next image after initialization")
    void loaderShouldHaveNextImageAfterInitialization() {
        Assertions.assertTrue(loader.hasNext());
    }

    @Test
    @DisplayName("Mask should have the same number of pixels as image")
    void MaskShouldHaveTheSameNumberOfPixelsAsImage() {
        Image<FloatType> image = loader.next();

        Cursor<FloatType> imageCursor = image.getImg().cursor();
        Cursor<NativeBoolType> maskCursor = image.getMaskImg().cursor();

        int imageCount = 0;
        while(imageCursor.hasNext()) {
            imageCursor.fwd();
            imageCount++;
        }
        int maskCount = 0;
        while(maskCursor.hasNext()) {
            maskCursor.fwd();
            maskCount++;
        }

        Assertions.assertEquals(imageCount, maskCount);
    }

    @Test
    @DisplayName("Masked image should have correct number of non-zero pixels")
    void maskedImageShouldHaveCorrectNumberOfNonZeroPixels() {
        Image<FloatType> image = loader.next();
        Img<FloatType> maskedImage = image.getMaskedImage();

        Cursor<FloatType> cursor = maskedImage.cursor();
        int actualNonZero = 0;
        while(cursor.hasNext()) {
            cursor.fwd();
            if (cursor.get().get() > 0) {
                actualNonZero++;
            }
        }

        Assertions.assertEquals(expectedNonZero, actualNonZero);
    }
}
