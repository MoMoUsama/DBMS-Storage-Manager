public class Key implements Comparable<Key> {
    private int id;
    private String name;

    public Key(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(Key other) {
        int cmp = Integer.compare(this.id, other.id);
        if (cmp != 0) return cmp;
        return this.name.compareTo(other.name);  // tie-breaker
    }

    @Override
    public String toString() {
        return id + ":" + name;
    }
}
