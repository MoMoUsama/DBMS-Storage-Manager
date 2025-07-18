public class Page {
    private int page_id;
    public int pin_count;
    public boolean  is_dirty;
    public byte [] buffer;
    private static int st_ID=0;

    // Fixed-size buffer (typical DB page sizes: 4KB, 8KB, etc.)
    public static final int PAGE_SIZE = 4096;

    public Page(int id)
    {
        this.page_id = id;
        this.is_dirty = false;
        this.buffer = new byte[PAGE_SIZE];
    }
    public Page()
    {
        this(st_ID++);
    }
    public int getPage_id() {
        return this.page_id;
    }
}
