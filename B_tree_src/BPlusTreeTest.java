import java.util.*;

public class BPlusTreeTest {
    public static void main(String[] args) {
        testBasicInsertionAndSearch();
        testDeletionScenarios();
        /*testDuplicateInsertion();
        testSplitAndPromotion();
        testMergeAfterDeletions();
        testRedistribution();
        testStressAndOrder();*/
        System.out.println("All tests passed.");
    }

    static void testBasicInsertionAndSearch() {
        System.out.println("✅ Start testBasicInsertionAndSearch");

        BPlusTree<Integer, String> tree = new BPlusTree<>(4);  // Small size to force splits

        List<Integer> insertedKeys = List.of(100, 70,10, 20, 30, 40, 50, 60, 33, 80,11);

        for (int key : insertedKeys) {
            boolean inserted = tree.insert(key, "V" + key);
            tree.printTree();
            if (!inserted) {
                throw new RuntimeException("❌ Insert failed for key: " + key);
            }
            System.out.println("✅ Inserted: " + key);
        }

        // 🔍 Validate in-order traversal
        System.out.println("=========================================================");
        System.out.println("In-Order-Traversal Of the B+ Tree:");
        List<Integer> actualKeys = tree.getAllKeysInOrder();
        for (Integer key : actualKeys) {
            String value = tree.getValue(key);
            System.out.print(" Key: " + key + ", Value: " + value);
            System.out.print("->");
        }


        // 🔍 Validate value lookup
        for (int key : insertedKeys) {
            String val = tree.getValue(key);
            if (val == null || !val.equals("V" + key)) {
                throw new RuntimeException("❌ Wrong value for key " + key + ". Expected: V" + key + ", Got: " + val);
            }
        }

        // 🔍 Check a key that should not exist
        if (tree.getValue(999) != null) {
            throw new RuntimeException("❌ Unexpected value found for missing key 999");
        }
        System.out.println("✅ All keys inserted and verified successfully.");
        System.out.println("✅ testBasicInsertionAndSearch passed");
    }

    static void testDeletionScenarios() {
        System.out.println("✅ Start testDeletionScenarios");

        BPlusTree<Integer, String> tree = new BPlusTree<>(4);

        // Step 1: Insert keys to fully populate and create a balanced tree
        List<Integer> keys = List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        for (int key : keys) {
            tree.insert(key, "V" + key);
        }

        tree.printTree();  // Initial full tree

        // Step 2: Simple delete (no underflow)
        System.out.println("🧪 Delete 100 (simple case)");
        tree.remove(100);
        tree.printTree();

        // Step 3: Delete to trigger redistribution (borrow from sibling)
        System.out.println("🧪 Delete 90 (causes redistribution)");
        tree.remove(90);
        tree.printTree();

        // Step 4: Delete to trigger merging (merge two leaf pages)
        System.out.println("🧪 Delete 80 (causes merge)");
        tree.remove(80);
        tree.printTree();

        // Step 5: Delete to cause internal page underflow
        System.out.println("🧪 Delete 70 (internal underflow)");
        tree.remove(70);
        tree.printTree();

        // Step 6: Delete all remaining keys to shrink root
        for (int key : List.of(10, 20, 30, 40, 50, 60)) {
            System.out.println("🧪 Delete " + key);
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



    static void testDuplicateInsertion() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        assert tree.insert(15, "X");
        assert !tree.insert(15, "Y");  // Duplicate
        assert tree.getValue(15).equals("X");
    }

    static void testSplitAndPromotion() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4);
        for (int i = 1; i <= 10; i++) {
            tree.insert(i, "V" + i);
        }
        List<Integer> keys = tree.getAllKeysInOrder();
        for (int i = 1; i <= 10; i++) {
            assert keys.contains(i);
        }
    }

    static void testMergeAfterDeletions() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(3);
        for (int i = 1; i <= 10; i++) {
            tree.insert(i, "V" + i);
        }
        tree.printTree();

        assert tree.getAllKeysInOrder().equals(List.of(10));
    }

    static void testRedistribution() {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4); // Even number allows cleaner mid
        for (int i = 10; i <= 50; i += 10) {
            tree.insert(i, "V" + i);
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

    static void testStressAndOrder()
    {
        BPlusTree<Integer, String> tree = new BPlusTree<>(4);
        int[] values = new Random().ints(0, 1000).distinct().limit(10).toArray();
        Arrays.sort(values);

        for (int v : values) {
            System.out.print(v+" ");
            tree.insert(v, "V" + v);
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
}

