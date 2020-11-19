package be.maximl.app.imglib;

import be.maximl.bf.lib.BioFormatsImage;
import be.maximl.bf.lib.BioFormatsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class ImageProducer extends Thread {

    final private BioFormatsLoader loader;
    final private BlockingQueue<BioFormatsImage> queue;
    final private Logger log = LoggerFactory.getLogger(ImageProducer.class);

    public ImageProducer(BioFormatsLoader loader, BlockingQueue<BioFormatsImage> queue) {
        this.loader = loader;
        this.queue = queue;
    }

    @Override
    public void run() {

        int full = 0;
        try {
            while(this.loader.hasNext()) {
                synchronized (queue) {
                    if (queue.remainingCapacity() == 0) {
                        full++;
                    }
                }
                queue.put(this.loader.next());
            }
            synchronized (queue) {
                while(!queue.isEmpty()) {
                    queue.wait();
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted");
        } finally {
            log.info("Encountered full queue " + full + " times.");
        }
    }
}
