import java.util.*;

public class Test<Key extends Comparable<Key>, RID> {

    private void insert(List<Integer> keys, BPlusTree<Integer, Integer> tree) {
        for (int key : keys) {
            boolean inserted = tree.insert(key, key + 100);
            if (!inserted) {
                throw new RuntimeException("❌ Insert failed for key: " + key);
            }
            System.out.println("✅ Inserted: " + key);
            tree.printTree();
        }

        // 🔍 Validate in-order traversal
        System.out.println("=========================================================");
        System.out.println("In-Order-Traversal Of the B+ Tree:");
        List<Integer> actualKeys = tree.getAllKeysInOrder();
        for (Integer key : actualKeys) {
            Integer value = tree.getValue(key);
            System.out.print(" Key: " + key + ", Value: " + value);
            System.out.print("->");
        }
    }

    public void testInsertAndGetAllKeys() {
        System.out.println("0- Start testInsertAndGetAllKeys() Test");
        BufferPoolManager bpm = new BufferPoolManager(100, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        // Insert keys
        List<Integer> insertedKeys = List.of(100, 70, 10, 20, 30, 40, 50, 60, 33, 80, 11);
        insert(insertedKeys, tree);
        tree.printTree();
        // 🔍 Validate value lookup
        for (int key : insertedKeys) {
            Integer val = tree.getValue(key);
            if (val == null || !val.equals(100 + key)) {
                throw new RuntimeException("❌ Wrong value for key " + key + ". Expected: V" + (key+100) + ", Got: " + val);
            }
        }

        // 🔍 Check a key that should not exist
        if (tree.getValue(999) != null) {
            throw new RuntimeException("❌ Unexpected value found for missing key 999");
        }
        System.out.println("✅ testBasicInsertionAndSearch passed");
    }

    public void testDeletionScenarios() {
        BufferPoolManager bpm = new BufferPoolManager(100, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        List<Integer> keys = List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        insert(keys, tree);
        System.out.println("\n ✅ Start testDeletionScenarios");

        // Step 2: Simple delete (no underflow)
        System.out.println("[testDeletionScenarios]  Delete 100 (simple case)");
        tree.remove(100);
        tree.printTree();

        // Step 3: Delete to trigger redistribution (borrow from sibling)
        System.out.println("[testDeletionScenarios]Delete 90 (simple case)");
        tree.remove(90);
        tree.printTree();

        // Step 4: Delete to trigger merging (merge two leaf pages)
        System.out.println("[testDeletionScenarios] Delete 80 (Leaf Underflow, do merging)");
        tree.remove(80);
        tree.printTree();

        // Step 5: Delete to cause internal page underflow
        System.out.println("[testDeletionScenarios] Delete 70 (simple case)");
        tree.remove(70);
        tree.printTree();

        // Step 6: Delete all remaining keys to shrink root
        for (int key : List.of(10, 20, 30, 40, 50, 60)) {
            System.out.println("[testDeletionScenarios] Delete " + key);
            tree.remove(key);
            tree.printTree();
        }

        // Step 7: Delete non-existent key
        boolean removed = tree.remove(999);
        if (removed) {
            throw new RuntimeException("❌ Deletion of non-existent key succeeded.");
        }

        System.out.println("✅ Deletion tests completed successfully.");
    }

    public void testDuplicateInsertion() {
        BufferPoolManager bpm = new BufferPoolManager(100, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        assert tree.insert(15, 115);
        assert !tree.insert(15, 120);  // Duplicate
        assert tree.getValue(15).equals(115);
    }

    public void testMergeAfterDeletions() {
        BufferPoolManager bpm = new BufferPoolManager(3, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        // Insert keys
        List<Integer> insertedKeys = List.of(100, 70, 10, 20, 30, 40, 50, 60, 33, 80, 11, 25, 35, 55, 72, 82, 92);
        insert(insertedKeys, tree);
        tree.printTree();
        // 🔍 Validate value lookup
        for (int key : insertedKeys) {
            Integer val = tree.getValue(key);
            if (val == null || !val.equals(100 + key)) {
                throw new RuntimeException("❌ Wrong value for key " + key + ". Expected: V" + (key+100) + ", Got: " + val);
            }
        }
        assert tree.getAllKeysInOrder().equals(List.of(17));
    }

    public void testRedistribution() {
        BufferPoolManager bpm = new BufferPoolManager(100, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        for (int i = 10; i <= 50; i += 10) {
            tree.insert(i, 100 + i);
        }

        // Delete to cause redistribution
        tree.remove(10);
        tree.remove(20);
        tree.remove(30);

        List<Integer> keys = tree.getAllKeysInOrder();
        assert keys.contains(40);
        assert keys.contains(50);
        assert !keys.contains(10);
        assert !keys.contains(20);
        assert !keys.contains(30);
    }

    public void testStressAndOrder() {
        BufferPoolManager bpm = new BufferPoolManager(10, 3); // size 100 pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to force splits
        int[] values = new Random().ints(0, 1000).distinct().limit(20).toArray();
        Arrays.sort(values);

        for (int v : values) {
            System.out.print(v);
            tree.insert(v, 100+v);
        }

        tree.printTree();

        List<Integer> keys = tree.getAllKeysInOrder();
        assert keys.equals(Arrays.stream(values).boxed().toList());

        // Remove half
        for (int i = 0; i < 50; i++) {
            tree.remove(values[i]);
        }

        List<Integer> afterRemove = tree.getAllKeysInOrder();
        for (int i = 0; i < 50; i++) {
            assert !afterRemove.contains(values[i]);
        }
    }
    public void testPersistenceCorrectness() {
        BufferPoolManager bpm = new BufferPoolManager(10, 3); // limited pool
        BPlusTree<Integer, Integer> tree = new BPlusTree<>(4, bpm); // small page size to trigger splits

        Map<Integer, Integer> expected = new TreeMap<>();
        int[] values = new Random().ints(0, 1000).distinct().limit(20).toArray();
        Arrays.sort(values);

        // Insert and record expected mappings
        for (int key : values) {
            int val = 100 + key;
            tree.insert(key, val);
            expected.put(key, val);
        }

        // Force all pages to disk
        tree.flushAll();

        // Fetch all keys back and validate order
        List<Integer> actualKeys = tree.getAllKeysInOrder();
        List<Integer> expectedKeys = Arrays.stream(values).boxed().toList();

        assert actualKeys.equals(expectedKeys) : "Key order mismatch after flush";

        // Validate all values can be retrieved correctly
        for (int key : expected.keySet()) {
            Integer result = tree.getValue(key);
            assert result != null : "Missing value for key " + key;
            assert result.equals(expected.get(key)) : "Incorrect value for key " + key;
        }

        System.out.println("✅ testPersistenceCorrectness passed.");
    }

}
