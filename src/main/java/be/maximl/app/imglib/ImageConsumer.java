package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import jdk.nashorn.internal.ir.Block;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class ImageConsumer extends Thread {

    final static private int LOGINTERVAL = 500;
    final private BlockingQueue<BioFormatsImage> queue;
    final private Logger log = LoggerFactory.getLogger(ImageConsumer.class);
    final private FeatureVectorFactory factory = new FeatureVectorFactory();
    private final Queue<FeatureVectorFactory.FeatureVector> writeQueue;

    public ImageConsumer(BlockingQueue<BioFormatsImage> queue, Queue<FeatureVectorFactory.FeatureVector> writeQueue) {
        this.queue = queue;
        this.writeQueue = writeQueue;
    }

    public void run() {

        long start = System.currentTimeMillis();
        long end;
        double rate;
        FeatureVectorFactory.FeatureVector vec;
        int consumed = 0;
        try {
            while(!this.isInterrupted()) {
                BioFormatsImage image = queue.take();
                vec = factory.computeVector(image);
                writeQueue.add(vec);
                synchronized (writeQueue) {
                    writeQueue.notify();
                }

                consumed++;
                if (consumed % LOGINTERVAL == 0) {
                    end = System.currentTimeMillis();
                    rate = (float)(end - start) / 1000*LOGINTERVAL;

                    log.info("Consumption rate: " + rate + "/s");
                    start = end;
                }

                synchronized (queue) {
                    queue.notify();
                }
            }
        } catch (InterruptedException e) {
            log.error("ImageConsumer interrupted.", e);
        } finally {
            log.info("Consumed " + consumed + " images.");
        }
    }

}
