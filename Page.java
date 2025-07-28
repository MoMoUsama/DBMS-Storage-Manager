import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Page {
    private int page_id;
    public int pin_count;
    public boolean  is_dirty;
    public byte [] buffer;

    // Fixed-size buffer (typical DB page sizes: 4KB, 8KB, etc.)
    public static final int PAGE_SIZE = 4096;

    public Page(int id)
    {
        this.page_id = id;
        this.is_dirty = false;
        this.buffer = new byte[PAGE_SIZE];
    }
    public int getPageId() {
        return this.page_id;
    }
    public byte[] getData() {
        return this.buffer;
    }
    public void setData(byte[] data) {this.buffer = data;}
    public int getSize(){ return this.buffer.length; }
    @Override
    public String toString() {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        int pageType = bb.getInt();
        int pageId = bb.getInt();
        int size = (pageType == 1) ? bb.getInt() : bb.getInt();
        int maxSize = (pageType == 1) ? bb.getInt() : bb.getInt();
        int nextPageId = (pageType == 1) ? bb.getInt() : -1; // Only in leaf

        StringBuilder sb = new StringBuilder();
        sb.append("Type: "+pageType).append("\nthe size is: ").append(size)
                .append("\nPageID: ").append(pageId);

        if (pageType == 1) { // Leaf
            sb.append("\nnextID: ").append(nextPageId);
        }

        sb.append("\nMaxSize: ").append(maxSize).append(" (");
        for (int i = 0; i < size; i++) {
            int key = bb.getInt();
            int val = bb.getInt();
            sb.append("(").append(key).append(",").append(val).append(")");
            if (i != size - 1) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

}
