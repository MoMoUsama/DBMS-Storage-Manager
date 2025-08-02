import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BufferPoolManager {
    private Map<Integer,Integer> PageTable; //<PageID, FrameID>
    private Set<Integer> UsedPageIDs;
    Page[] frames;
    private int nextPageId = 0;
    public Map<Integer,Boolean> UsedFrames; //<frameID,Used>
    public LRUKReplacer lruk;
    public DiskScheduler disk_sch;
    private int BoolSize;

    public BufferPoolManager(int BoolSize, int kForLRU){
        try {
            lruk = new LRUKReplacer(kForLRU);
            disk_sch = new DiskScheduler();
            this.BoolSize = BoolSize;
            this.frames = new Page[this.BoolSize];
            this.UsedFrames = new HashMap<>();
            this.PageTable = new HashMap<>();
            this.UsedPageIDs = new HashSet<>();
        }
        catch(Exception e)
        {
            System.out.println("Failed while Creating BufferPoolManager, "+e);
        }
    }

    public synchronized Page FetchPage(int pageId) throws Exception
    {
        //System.out.println("[FetchPage] The Required ID is: "+pageId);
        //System.out.println("[FetchPage] Frames[]: ");
        //printFrames();
        Page fetchedPage;
        if(PageTable.containsKey(pageId))
        {
            int frameIdx = PageTable.get(pageId);
            fetchedPage = frames[frameIdx];
            fetchedPage.pin_count++;
            lruk.recordAccess(frameIdx);
            return fetchedPage;
        }
        else {
            try {
                int freeFrame = getFreeFrame();
                if (freeFrame == -1) {
                    throw new RuntimeException("[BufferPoolManager/FetchPage] There is no Free Frame");
                }

                fetchedPage = new Page(pageId);
                frames[freeFrame] = fetchedPage;
                PageTable.put(pageId, freeFrame);
                fetchedPage.pin_count++;

                if(UsedPageIDs.contains(pageId)) {
                    //  perform read request //
                    DiskRequest read_rqst = new DiskRequest(DiskRequest.RequestType.READ, pageId, fetchedPage.buffer);
                    read_rqst.getCompletionFuture();
                    disk_sch.schedule(read_rqst);
                    read_rqst.getCompletionFuture().get();
                }
                else {
                    UsedPageIDs.add(pageId);
                }
                lruk.recordAccess(freeFrame);
                return fetchedPage;
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        }
        return null;
    }
    public void printFrames() {
        System.out.println("=== Buffer Pool Frames ===");
        for (int i = 0; i < frames.length; i++) {
            Page page = frames[i];
            if (page != null) {
                System.out.println("Frame " + i + ": Page ID = " + page.getPageId());
                System.out.println(page.toString());
            } else {
                System.out.println("Frame " + i + ": EMPTY");
            }
        }
    }
    public synchronized void UnpinPage(int pageId, boolean isDirty) throws Exception {
        if (!PageTable.containsKey(pageId)) {
            throw new Exception("UnpinPage: Page not found in buffer pool: " + pageId);
        }
        int frameID = PageTable.get(pageId);
        Page target = frames[frameID];
        target.pin_count--;
        target.is_dirty = isDirty;
        if(target.pin_count==0)
        {
            flushPage(pageId);
            lruk.setEvictable(frameID,true);
        }
    }

    private int getFreeFrame() {
        for (int i = 0; i < BoolSize; i++) {
            if (UsedFrames.containsKey(i)) continue;
            UsedFrames.put(i, true);
            return i;
        }

        // No free frame, ask replacer for one
        int[] frame = new int[1];
        boolean evicted = lruk.evict(frame);
        if (!evicted) return -1; //no evictable frame

        int victimFrameId = frame[0];
        Page victim = frames[victimFrameId];
        if (victim.pin_count > 0) return -1; //the victim already in use

        if (victim.is_dirty) {
            try {
                flushPage(victim.getPageId()); // wait for flush
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        UsedFrames.remove(victimFrameId);
        PageTable.remove(victim.getPageId());
        return frame[0];
    }

    public synchronized Page newPage(){
        int frameId = getFreeFrame();
        try {
            if (frameId == -1)
                throw new RuntimeException("[BufferPoolManager] Cannot Find Free Frame, there is no evictable frames");
            int pageId = nextPageId++;
            Page page = new Page(pageId);
            page.pin_count = 1;

            frames[frameId] = page;
            PageTable.put(pageId, frameId);
            lruk.recordAccess(frameId);
            UsedFrames.put(frameId, true);
            /*// ✅ Debug Print
            System.out.println("✅ New page allocated: PageID=" + pageId + ", FrameID=" + frameId);
            System.out.println("📘 PageTable:");
            for (Map.Entry<Integer, Integer> entry : PageTable.entrySet()) {
                System.out.println("  PageID " + entry.getKey() + " -> FrameID " + entry.getValue());
            }

            System.out.println("📦 Frames:");
            for (int i = 0; i < frames.length; i++) {
                Page p = frames[i];
                if (p != null)
                    System.out.println("  Frame[" + i + "] = PageID " + p.getPageId() + ", pin_count=" + p.pin_count + ", dirty=" + p.is_dirty);
                else
                    System.out.println("  Frame[" + i + "] = null");
            }*/
            return page;
        }
        catch(Exception e)
        {
            System.out.println(e);
            return null;
        }
    }

    private void flushPage (int pageID) throws Exception
    {
        if (!PageTable.containsKey(pageID)) {
            throw new Exception("flushPage: Page not found in buffer pool: " + pageID);
        }
        int frameId = PageTable.get(pageID);
        Page victim = frames[frameId];
        if (victim.is_dirty) //schedule write request
        {
            DiskRequest write_rqst = new DiskRequest(DiskRequest.RequestType.WRITE, victim.getPageId(), victim.buffer);
            //write_rqst.getCompletionFuture();  // <- ensure DiskRequest has a future
            disk_sch.schedule(write_rqst);
            write_rqst.getCompletionFuture().get(); //request served correctly
            victim.is_dirty = false; //now it is not dirty

        }
    }

    public synchronized boolean DeletePage(int pageId) throws Exception {
        if (!PageTable.containsKey(pageId)) {
            throw new Exception("DeletePage: Page not found in buffer pool: " + pageId);
        }
        int frameID = PageTable.get(pageId);
        Page victim = frames[frameID];
        if (victim.pin_count == 0) {
            if(victim.is_dirty)
                flushPage(pageId);
            PageTable.remove(pageId);
            UsedFrames.remove(frameID);
            frames[frameID]= null;
            lruk.remove(frameID); //remove entirely
            return true;
        }
        return false;
    }

    public synchronized void flushAllPages () throws Exception
    {
        for(Integer pageId :PageTable.keySet())
            flushPage(pageId);
    }
}
