import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BufferPoolManager {
    private Map<Integer,Integer> PageTable; //<PageID, FrameID>
    Page[] frames;
    public Map<Integer,Boolean> UsedFrames; //<frameID,Used>
    public LRUKReplacer lruk;
    public DiskScheduler disk_sch;
    private int BoolSize;

    public BufferPoolManager(int BoolSize) throws IOException {
        lruk = new LRUKReplacer( 3);
        disk_sch = new DiskScheduler();
        this.BoolSize = BoolSize;
        this.frames = new Page[BoolSize];
        this.UsedFrames = new HashMap<>();
        this.PageTable = new HashMap<>();
    }

    public synchronized Page FetchPage(int pageId) throws Exception
    {
        Page fetchedPage;
        if(PageTable.containsKey(pageId))
        {
            int idx = PageTable.get(pageId);
            fetchedPage = frames[idx];
            fetchedPage.pin_count++;
            lruk.recordAccess(idx);
            return fetchedPage;
        }
        else
        {
            int foundedFrame = getFreeFrame();
            if(foundedFrame==-1) return null;

            fetchedPage = new Page(pageId);
            frames[foundedFrame] = fetchedPage;
            PageTable.put(pageId,foundedFrame);
            fetchedPage.pin_count++;

            //  perform read request //
            DiskRequest read_rqst = new DiskRequest(DiskRequest.RequestType.READ, pageId, fetchedPage.buffer);
            read_rqst.getCompletionFuture();
            disk_sch.schedule(read_rqst);
            read_rqst.getCompletionFuture().get();
            lruk.recordAccess(foundedFrame);
            return fetchedPage;
        }
    }

    public synchronized void UnpinPage(int pageId, boolean isDirty) throws Exception {
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
                flushPage(victim.getPage_id()); // wait for flush
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        return frame[0];
    }


    public synchronized Page newPage()
    {
        Page target = new Page();
        target.pin_count=1;
        int founedFrame = getFreeFrame();
        if(founedFrame==-1) return null;
        frames[founedFrame]= target;
        PageTable.put(target.getPage_id(), founedFrame);
        return target;
    }

    private void flushPage (int pageID) throws Exception
    {
        int frameId = PageTable.get(pageID);
        Page victim = frames[frameId];
        if (victim.is_dirty) //schedule write request
        {
            DiskRequest write_rqst = new DiskRequest(DiskRequest.RequestType.WRITE, victim.getPage_id(), victim.buffer);
            write_rqst.getCompletionFuture();  // <- ensure DiskRequest has a future
            disk_sch.schedule(write_rqst);
            write_rqst.getCompletionFuture().get(); //request served correctly
            victim.is_dirty = false; //now it is not dirty

        }
    }

    public synchronized void DeletePage(int pageId) throws Exception {
        if(PageTable.containsKey(pageId))
        {
            int frameID = PageTable.get(pageId);
            Page victim = frames[frameID];
            if (victim.pin_count == 0) {
                if(victim.is_dirty)
                    flushPage(pageId);
                PageTable.remove(pageId);
                UsedFrames.remove(frameID);
                frames[frameID]= null;
                lruk.remove(frameID); //remove entirely
            }
        }
    }

    public synchronized void flushAllPages () throws Exception
    {
        for(Integer pageId :PageTable.keySet())
            flushPage(pageId);
    }
}
