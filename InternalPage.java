import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Stack;
import java.nio.ByteBuffer;
public class InternalPage<Key extends Comparable<Key>> extends BPlusTreePage {
    private Key[] keyArray;
    private int[] pageIdArray;

    @SuppressWarnings("unchecked")
    public void init(int maxSize, int ID) {
        setPageType(Types.PageType.INTERNAL_PAGE);
        setMaxSize(maxSize);
        setSize(0);
        setPageID(ID);
        // Initialize arrays
        this.keyArray = (Key[]) new Comparable[maxSize];
        this.pageIdArray = new int[maxSize + 1];
    }

    public Key keyAt(int index) {
        return keyArray[index];
    }

    public void setKeyAt(int index, Key key) {
        keyArray[index] = key;
    }

    public int valueAt(int index) {
        return pageIdArray[index];
    }

    public void setValueAt(int index, int value) {
        pageIdArray[index] = value;
    }

    public int valueIndex(int value) {
        for (int i = 0; i <= getSize(); i++) {
            if (pageIdArray[i]==value) return i;
        }
        return -1;
    }

    public boolean insertEntry(Key key, int rightPageId) {
        if (getSize() >= getMaxSize()) {
            return false;
        }

        // Step 1: Find the correct insert position
        int pos = 1;
        while (pos <= getSize() && key.compareTo(keyArray[pos]) > 0) {
            pos++;
        }

        // Step 2: Shift keys and children right
        for (int i = getSize(); i >= pos; i--) {
            keyArray[i + 1] = keyArray[i];
        }
        for (int i = getSize() + 1; i >= pos + 1; i--) {
            pageIdArray[i] = pageIdArray[i - 1];
        }

        keyArray[pos] = key;
        pageIdArray[pos] = rightPageId;

        setSize(getSize() + 1);

        return true;
    }

    public boolean removeEntry(int keyIndex) {
        if (keyIndex < 1 || keyIndex > getSize()) {
            return false;
        }
        for (int i = keyIndex; i<getSize(); i++) {
            keyArray[i] = keyArray[i + 1];
        }

        for (int i = keyIndex; i < getSize(); i++) {
            pageIdArray[i] = pageIdArray[i + 1];
        }
        pageIdArray[getSize()] = -1;
        keyArray[getSize()] = null;

        setSize(getSize() - 1);
        return true;
    }
    public int binarySearch( Key key) //looks for the index of a key
    {
        // Binary search for efficiency with many keys
        int low = 1;
        int high = this.getSize();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Key midKey = this.keyAt(mid);
            int cmp = key.compareTo(midKey);

            if (cmp < 0) {
                high = mid - 1;
            } else if (cmp > 0) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return low-1; // Return the first key greater than search key
    }

    public Key splitInternalPage(InternalPage<Key> newInternal) {
        int totalSize = this.getSize();
        int mid = totalSize / 2;
        Key midKey = this.keyAt(mid);

        // Copy keys: from mid + 1 to totalSize - 1 (inclusive)
        for (int i = mid + 1, j = 1; i <= totalSize; i++, j++) {
            newInternal.setKeyAt(j, this.keyAt(i));
        }

        // Copy page IDs: from mid + 1 to totalSize (inclusive)
        for (int i = mid, j = 0; i <= totalSize; i++, j++) {
            newInternal.setValueAt(j, this.valueAt(i));
        }

        // Set sizes
        newInternal.setSize(totalSize - mid);
        this.setSize(mid);

        return midKey;
    }
    public Key[] getKeys()
    {
        return keyArray;
    }
    public int[] getPageIDs(){ return pageIdArray;}

    public String toString() {
        StringBuilder sb = new StringBuilder("the size is: "+this.size
                + "\n PageID: "+this.getPageID()
                +"\n MaxSize: " +this.maxSize +"(");
        sb.append("Keys:  ");
        for (int i = 1; i <= getSize(); i++) {
            sb.append(keyArray[i].toString()+" ");
        }
        sb.append("IDs: ");
        for (int i = 0; i <= getSize(); i++) {
            sb.append(pageIdArray[i]+" ");
        }
        return sb.toString();
    }

    public static final int INTERNAL_PAGE_TYPE = 2;
    public byte[] toBytes() {
        int n = this.getSize();
        int totalBytes = (4*4) + (n * 4) + ((n + 1) * 4); // type + pageID + keyCount + maxSize + keys + pageId
        ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
        buffer.putInt(INTERNAL_PAGE_TYPE); // 4 bytes (type)
        buffer.putInt(this.getPageID());   // 4 bytes (PageID)
        buffer.putInt(n);                  // 4 bytes (size)
        buffer.putInt(this.getMaxSize());  // 4 bytes (maxSize)

        // Put keys
        for (int i = 1; i <= n; i++) {
            buffer.putInt((Integer) this.keyAt(i));
        }

        // Put page IDs
        for (int i = 0; i <= n; i++) {
            buffer.putInt(this.valueAt(i)); // 4 bytes each
        }
        return buffer.array();
    }
    public void fromBytes(ByteBuffer buffer) {
        int id  = buffer.getInt();
        int size = buffer.getInt(); // number of keys
        int maxSize = buffer.getInt();
        this.init(maxSize, id);
        this.setSize(size);
        for (int i = 1; i <= size; i++) {
            Key key = (Key) Integer.valueOf(buffer.getInt());
            this.setKeyAt(i, key);
        }
        for (int i = 0; i <= size; i++) {
            this.setValueAt(i, buffer.getInt());
        }
    }
}
