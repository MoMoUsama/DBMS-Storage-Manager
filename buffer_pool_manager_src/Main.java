import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        runSingleThreadedTest();
        //runMultiThreadedTest();
    }

    public static void runSingleThreadedTest() throws Exception {
        System.out.println("🔧 Running Single Threaded Test...");
        int poolSize = 3;

        BufferPoolManager bpm = new BufferPoolManager(poolSize);

        Page page1 = bpm.newPage();
        Page page2 = bpm.newPage();
        Page page3 = bpm.newPage();

        assert page1 != null && page2 != null && page3 != null : "Page allocation failed";

        // Modify and unpin page1
        page1.buffer[0] = 99;
        bpm.UnpinPage(page1.getPage_id(), true);
        bpm.UnpinPage(page2.getPage_id(), false);
        bpm.UnpinPage(page3.getPage_id(), false);

        // Re-fetch page1
        Page fetched = bpm.FetchPage(page1.getPage_id());
        assert fetched.buffer[0] == 99 : "Data mismatch after fetch";
        bpm.UnpinPage(page2.getPage_id(), false);

        // This should trigger eviction
        Page page4 = bpm.newPage();
        assert page4 != null : "Eviction failed to free space";

        bpm.flushAllPages();
        bpm.DeletePage(page2.getPage_id());
        System.out.println("✅ Single Threaded Test Passed!\n");
    }
}