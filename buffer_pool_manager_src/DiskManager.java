import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class DiskManager {
    private static final int PAGE_SIZE = 4096;
    private final RandomAccessFile file;

    public DiskManager(String fileName) throws IOException {
        File f = new File(fileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        file = new RandomAccessFile(f, "rw");
    }

    public synchronized void writePage(int pageId, byte[] data) throws IOException {
        if (data.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Data must be exactly one page (" + PAGE_SIZE + " bytes)");
        }
        file.seek((long) pageId * PAGE_SIZE); //(offset within the file)
        file.write(data);
    }

    public synchronized void readPage(int pageId, byte[] buffer) throws IOException {
        if (buffer.length != PAGE_SIZE) {
            throw new IllegalArgumentException("Buffer must be exactly one page (" + PAGE_SIZE + " bytes)");
        }
        long offset = (long) pageId * PAGE_SIZE;
        if (offset >= file.length()) {
            throw new IllegalArgumentException("Offset Must be within the file length, offset = " + offset +
                    "  File Length = "+file.length());
        }

        file.seek(offset);
        int bytesRead = file.read(buffer);
        if (bytesRead < PAGE_SIZE) {
            Arrays.fill(buffer, bytesRead, PAGE_SIZE, (byte) 0); // Pad rest with zeros
        }
    }

    public synchronized void shutdown() throws IOException {
        file.close();
    }

    public static int getPageSize() {
        return PAGE_SIZE;
    }
}
