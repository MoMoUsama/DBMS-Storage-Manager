import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LRUKReplacer {
    private final int k;
    private long timestamp = 0;


    private Map<Integer, Deque<Long>> accessHistory; // Maps frameId to access timestamps
    private Map<Integer, Boolean> isEvictable; // <frameId, canBeEvicted>
    private Map<Integer, Long> oldestAccess; // Tracks earliest access <frameId, numOfAccess>
    private int currentSize; // Total number of evictable frames

    public LRUKReplacer(int k) {
        this.accessHistory = new HashMap<>();
        this.isEvictable = new HashMap<>();
        this.oldestAccess = new HashMap<>();
        this.k = k;
    }

    // Methods to implement
    public synchronized boolean evict(int[] frameId) {
        long maxDistance = Long.MIN_VALUE;
        int victim = -1;
        long oldest = Long.MAX_VALUE;
        boolean lessThanK = false ;

        for (int id : accessHistory.keySet()) {
            if (!isEvictable.getOrDefault(id, false)) continue; //not evictable

            Deque<Long> history = accessHistory.get(id);
            if (history.size() < k) {
                lessThanK = true;
                long firstAccess = oldestAccess.get(id);
                if (firstAccess < oldest) {
                    oldest = firstAccess;
                    victim = id;
                }
            } else if (!lessThanK && !history.isEmpty()){
                long distance = timestamp - history.peekFirst();
                if (distance > maxDistance) {
                    maxDistance = distance;
                    victim = id;
                }
            }
        }

        if (victim == -1) return false;

        frameId[0] = victim;
        accessHistory.remove(victim);
        isEvictable.remove(victim);
        oldestAccess.remove(victim);
        currentSize--;

        return true;
    }

    public synchronized void recordAccess(int frameId) {
        timestamp++;
        accessHistory.putIfAbsent(frameId, new LinkedList<>());
        Deque<Long> history  = accessHistory.get(frameId);
        history.addLast(timestamp);
        if(history.size()>k){
            history.removeFirst();
            oldestAccess.put(frameId,history.peekFirst());
        }
        oldestAccess.putIfAbsent(frameId,timestamp);   //it is the first access to this frame
    }


    public synchronized void remove(int frameId) {
        if(isEvictable.getOrDefault(frameId, false)){
            currentSize--;
            isEvictable.remove(frameId);
            oldestAccess.remove(frameId);
            accessHistory.remove(frameId);
        }
    }

    public synchronized void setEvictable(int frameId, boolean evectable) {
        if(evectable && isEvictable.getOrDefault(frameId,false))
            currentSize++;
        isEvictable.put(frameId, evectable);
    }

    public synchronized int size() {
        return currentSize;
    }
}
