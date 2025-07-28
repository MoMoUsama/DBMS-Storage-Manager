public abstract class BPlusTreePage {
    protected Types.PageType pageType;
     protected int size;    //current number of keys stored in the page
    protected int maxSize;
    private int pageID;
    public boolean isLeafPage() {
        return pageType == Types.PageType.LEAF_PAGE;
    }

    public int getPageID(){ return this.pageID; }
    public void setPageID(int pageID){ this.pageID=pageID; }

    public void setPageType(Types.PageType pageType) {
        this.pageType = pageType;
    }
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void changeSizeBy(int delta) {
        this.size += delta;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMinSize() {
        return maxSize / 2;
    }
}
