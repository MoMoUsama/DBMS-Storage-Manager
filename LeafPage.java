import java.nio.ByteBuffer;
import java.util.Arrays;

public class LeafPage<Key extends Comparable<Key>, RID> extends BPlusTreePage {
    private Key[] keyArray;
    private RID[] valueArray;
    public int nextPageId;

    @SuppressWarnings("unchecked")
    public void init(int maxSize, int ID) {
        setPageType(Types.PageType.LEAF_PAGE);
        setMaxSize(maxSize);
        setSize(0);
        setPageID(ID);
        // Initialize arrays
        this.keyArray = (Key[]) new Comparable[maxSize];
        this.valueArray = (RID[]) new Object[maxSize];
        nextPageId = -1;
    }

    public boolean containsKey(Key key)
    {
        for (int i = 0; i < this.getSize(); i++) {
            if (key.compareTo(this.keyAt(i)) == 0) return true;
        }
        return false;
    }

    public Key keyAt(int index) {
        return keyArray[index];
    }

    public void setKeyAt(int index, Key key) {
        keyArray[index] = key;
    }

    public RID valueAt(int index) {
        return valueArray[index];
    }

    public void setValueAt(int index, RID value) {
        valueArray[index] = value;
    }

    public int valueIndex(RID value) {
        for (int i = 0; i < getSize(); i++) {
            if (valueArray[i].equals(value)) return i;
        }
        return -1;
    }
    public int keyIndex(Key key)
    {
        for(int i=0 ; i<getSize(); i++)
        {
            if (key != null && key.compareTo(keyArray[i]) == 0) return i;
        }
        return -1;
    }

    public int getNextPageId() {
        return nextPageId;
    }

    public void setNextPageId(int nextPageId) {
        this.nextPageId = nextPageId;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("the size is: "+this.size
                                            + "\n PageID: "+this.getPageID()
                                             +"\n nextID: "+this.nextPageId
                                            +"\n MaxSize: " +this.maxSize +"(");

        for (int i = 0; i < getSize(); i++) {
            sb.append("("+keyArray[i].toString()+","+valueArray[i].toString()+")");
            if (i != getSize() - 1) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    public Key[] getKeyArray() {
        return keyArray;
    }
    public RID[] getValueArray() {
        return valueArray;
    }
    public void setKeyArray(Key[] keyArray) { this.keyArray=keyArray; }
    public void setValueArray(RID[] valueArray) {
        this.valueArray=valueArray;
    }

    public LeafPage<Key, RID> splitLeafPage(int newLeafId) {
        int mid = this.getSize() / 2;
        int total = this.getSize();
        LeafPage<Key, RID> newLeaf = new LeafPage<>();
        newLeaf.init(this.getMaxSize(), newLeafId);

        for (int i = mid; i < total; i++) {
            newLeaf.setKeyAt(i - mid, this.keyAt(i));
            newLeaf.setValueAt(i - mid, this.valueAt(i));
        }

        for (int i = mid; i < total; i++) {
            this.setKeyAt(i, null);
            this.setValueAt(i, null);
        }

        newLeaf.setSize(total - mid);
        this.setSize(mid);

        newLeaf.setNextPageId(this.getNextPageId());
        this.setNextPageId(newLeafId);

        return newLeaf;
    }


    public void insertIntoLeaf( Key key, RID value)
    {
       // System.out.println("[InsertIntoLeaf] the lEAF before insertion:\n"+this.toString());
        int pos = 0;
        while (pos < this.getSize() && key.compareTo(this.keyAt(pos)) > 0) pos++;

        for (int i = this.getSize(); i > pos; i--) {
            this.setKeyAt(i, this.keyAt(i - 1));
            this.setValueAt(i, this.valueAt(i - 1));
        }
        this.setKeyAt(pos, key);
        this.setValueAt(pos, value);
        this.size++;

    }

    public boolean remove(Key key) {
        int size = getSize();

        for (int i = 0; i < size; i++) {
            if (key.compareTo(keyArray[i]) == 0) {
                // Shift all keys and values left
                for (int j = i; j < size - 1; j++) {
                    keyArray[j] = keyArray[j + 1];
                    valueArray[j] = valueArray[j + 1];
                }

                // Null out the last element (optional but cleaner)
                keyArray[size - 1] = null;
                valueArray[size - 1] = null;

                setSize(size - 1);
                return true;
            }
        }

        return false;  // key not found
    }

    public byte[] toBytes() {
        // the object that will be serialized //
        int numEntries = this.getSize();
        ByteBuffer buffer = ByteBuffer.allocate((4*5) + (numEntries * 8)); // type + ID + nextID + size + maxSize + pairs
        buffer.putInt(1); // page type
        buffer.putInt(this.getPageID());   // page ID
        buffer.putInt(numEntries);        // page size
        buffer.putInt(this.maxSize);     // page max size
        buffer.putInt(this.nextPageId); // next pointer

        for (int i = 0; i < numEntries; i++) {
            buffer.putInt((Integer) this.getKeyArray()[i]);
            buffer.putInt((Integer) this.getValueArray()[i]);
        }
        return buffer.array();
    }
    public void fromBytes(ByteBuffer buffer) {
        try {
            int pageID = buffer.getInt();
            int numEntries = buffer.getInt();
            int maxSize = buffer.getInt();
            int nextPageId = buffer.getInt();
            this.init(maxSize, pageID);
            this.setSize(numEntries);
            this.setNextPageId(nextPageId);
            if (numEntries != 0) {
                for (int i = 0; i < numEntries; i++) {
                    Key key = (Key) Integer.valueOf(buffer.getInt());
                    RID rid = (RID) Integer.valueOf(buffer.getInt());

                    this.setKeyAt(i, key);
                    this.setValueAt(i, rid);
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
