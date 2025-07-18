import java.util.concurrent.CompletableFuture;

public class DiskRequest {
    public enum RequestType {
        READ,
        WRITE
    }

    private final RequestType type;
    private final int pageId;
    private final byte[] buffer;  // For write: data to write; for read: will be filled
    private final CompletableFuture<Void> completionFuture;

    public DiskRequest(RequestType type, int pageId, byte[] buffer) {
        this.type = type;
        this.pageId = pageId;
        this.buffer = buffer;
        this.completionFuture = new CompletableFuture<>();
    }

    public RequestType getType() { return type; }
    public int getPageId() { return pageId; }
    public byte[] getBuffer() { return buffer; }
    public CompletableFuture<Void> getCompletionFuture() { return completionFuture; }
}