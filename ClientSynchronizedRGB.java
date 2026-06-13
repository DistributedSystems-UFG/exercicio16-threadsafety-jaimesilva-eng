import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSynchronizedRGB {
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;

    private static boolean isConsistent(int rgb, String name) {
        return (rgb == BLACK && name.equals("black")) ||
               (rgb == WHITE && name.equals("white"));
    }

    public static void main(String[] args) throws Exception {
        testWithoutExternalLock();
        testWithExternalLock();
    }

    private static void testWithoutExternalLock() throws Exception {
        SynchronizedRGB color = new SynchronizedRGB(0, 0, 0, "black");

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger inconsistencies = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }

            while (!stop.get()) {
                color.set(255, 255, 255, "white");
                color.set(0, 0, 0, "black");
            }
        });

        Runnable readerTask = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }

            for (int i = 0; i < 100_000 && !stop.get(); i++) {
                int rgb = color.getRGB();

                // Aumenta a chance de outra thread alterar o objeto entre os getters.
                Thread.yield();

                String name = color.getName();

                if (!isConsistent(rgb, name)) {
                    int total = inconsistencies.incrementAndGet();

                    if (total <= 5) {
                        System.out.printf(
                            "Inconsistencia sem lock externo: rgb=%06X, name=%s%n",
                            rgb,
                            name
                        );
                    }

                    stop.set(true);
                }
            }
        };

        Thread[] readers = new Thread[8];

        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(readerTask);
        }

        writer.start();

        for (Thread t : readers) {
            t.start();
        }

        start.countDown();

        for (Thread t : readers) {
            t.join();
        }

        stop.set(true);
        writer.join();

        System.out.println(
            "SynchronizedRGB sem lock externo - inconsistencias: "
            + inconsistencies.get()
        );
    }

    private static void testWithExternalLock() throws Exception {
        SynchronizedRGB color = new SynchronizedRGB(0, 0, 0, "black");

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicInteger inconsistencies = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }

            while (!stop.get()) {
                color.set(255, 255, 255, "white");
                color.set(0, 0, 0, "black");
            }
        });

        Runnable readerTask = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }

            for (int i = 0; i < 100_000; i++) {
                int rgb;
                String name;

                synchronized (color) {
                    rgb = color.getRGB();
                    Thread.yield();
                    name = color.getName();
                }

                if (!isConsistent(rgb, name)) {
                    inconsistencies.incrementAndGet();
                    stop.set(true);
                    break;
                }
            }
        };

        Thread[] readers = new Thread[8];

        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(readerTask);
        }

        writer.start();

        for (Thread t : readers) {
            t.start();
        }

        start.countDown();

        for (Thread t : readers) {
            t.join();
        }

        stop.set(true);
        writer.join();

        System.out.println(
            "SynchronizedRGB com lock externo - inconsistencias: "
            + inconsistencies.get()
        );
    }
}