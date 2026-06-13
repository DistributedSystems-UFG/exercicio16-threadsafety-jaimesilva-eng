import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientImmutableRGB {
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        ImmutableRGB black = new ImmutableRGB(0, 0, 0, "black");

        AtomicInteger inconsistencies = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);

        Runnable task = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                return;
            }

            for (int i = 0; i < 100_000; i++) {
                int rgbBefore = black.getRGB();
                String nameBefore = black.getName();

                ImmutableRGB inverse = black.invert();

                int rgbAfter = black.getRGB();
                String nameAfter = black.getName();

                boolean originalPreserved =
                    rgbBefore == BLACK &&
                    rgbAfter == BLACK &&
                    nameBefore.equals("black") &&
                    nameAfter.equals("black");

                boolean inverseCorrect =
                    inverse.getRGB() == WHITE &&
                    inverse.getName().equals("Inverse of black");

                if (!originalPreserved || !inverseCorrect) {
                    inconsistencies.incrementAndGet();
                    break;
                }
            }
        };

        Thread[] threads = new Thread[8];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
        }

        for (Thread t : threads) {
            t.start();
        }

        start.countDown();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println(
            "ImmutableRGB - inconsistencias: " + inconsistencies.get()
        );

        System.out.printf(
            "Objeto original ao final: rgb=%06X, name=%s%n",
            black.getRGB(),
            black.getName()
        );
    }
}