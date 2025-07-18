import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class DiskScheduler {
    private final BlockingQueue<DiskRequest> requestQueue = new LinkedBlockingQueue<>();
    private final Thread backgroundThread;
    private volatile boolean shutdown = false;
    private final DiskManager diskManager = new DiskManager("database.db");

    public DiskScheduler() throws IOException {
        backgroundThread = new Thread(this::startWorkerThread);
        backgroundThread.start();
    }

    public void schedule(DiskRequest request) {requestQueue.offer(request);} // Non-blocking enqueue


    private void startWorkerThread() {
        while (!shutdown || !requestQueue.isEmpty()) {
            try {
                DiskRequest request = requestQueue.take(); // blocks until available
                if(request.getType() == DiskRequest.RequestType.READ)
                {
                    try{
                        diskManager.readPage(request.getPageId(), request.getBuffer());
                        request.getCompletionFuture().complete(null);
                    } catch (Exception e) {
                        request.getCompletionFuture().completeExceptionally(e);
                    }
                }
                else
                {
                    try{
                        diskManager.writePage(request.getPageId(), request.getBuffer());
                        request.getCompletionFuture().complete(null);
                    } catch (Exception e) {
                        request.getCompletionFuture().completeExceptionally(e);
                    }
                }
            } catch (InterruptedException e) {
                if (shutdown) break;
            }
        }
    }

    public void shutdown() {
        shutdown = true;
        backgroundThread.interrupt(); // Wake the thread if blocked
        try {
            backgroundThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
