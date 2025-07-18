import java.util.*;

import static java.lang.Math.ceil;

public class BPlusTree<Key extends Comparable<Key>, RID> {
    private int rootPageId;
    private Map<Integer, BPlusTreePage> pageTable;  // <PageId, PageObject> if using integer IDs
    private int nextPageId=0; // For allocating new page IDs

    public BPlusTree(int maxsize) {
        this.pageTable = new HashMap<>();
        rootPageId = allocatePageId();
        LeafPage<Key, RID> root = new LeafPage<>();
        root.init(maxsize, rootPageId);
        pageTable.put(rootPageId, root);
    }
    public int getRootPageID(){
            return rootPageId;
    }
    public void setRootPageID(int rootID){
        this.rootPageId=rootID;
    }


    private int allocatePageId()
    {
        return nextPageId++;
    }

    public boolean insert(Key key, RID value)
    {
        Stack<BPlusTreePage> parentStack = new Stack<>();
        LeafPage<Key, RID> leaf = findLeafPage(key, parentStack); //get the correct leaf

        // Duplicate-key check
        if (leaf.containsKey(key)) {
            return false;
        }

        //  normal Case
        if (leaf.getSize() < leaf.getMaxSize()) {
            leaf.insertIntoLeaf(key, value);
            return true;
        }

        // Leaf is full → split + insert + push key up
        int newleafID = allocatePageId();
        LeafPage<Key, RID> newLeaf = leaf.splitLeafPage(newleafID);
        pageTable.put(newleafID, newLeaf); // Register the new leaf page

        if (key.compareTo(newLeaf.keyAt(0)) < 0) {
            leaf.insertIntoLeaf(key, value);
        } else {
            newLeaf.insertIntoLeaf(key, value);
        }

        // add new separator in the parent //
        Key promoteKey = newLeaf.keyAt(0);
        insertIntoParent(leaf, promoteKey, newLeaf, parentStack);
        System.out.println("[insert]: the page table has "+pageTable.size()+" entries");
        return true;
    }


    @SuppressWarnings("unchecked")
    public LeafPage<Key, RID> findLeafPage(Key key, Stack<BPlusTreePage> parentStack) //find the leaf that contains this key
    {
        BPlusTreePage currentPage = pageTable.get(rootPageId);
        while (!currentPage.isLeafPage()) {
            InternalPage<Key> internalPage = (InternalPage<Key>) currentPage;

            // Track parent pages if requested
            if (parentStack != null) {
                parentStack.push(internalPage);
            }

            int keyIndex = internalPage.binarySearch(key); //the key or first key greater
            int childPageId = internalPage.valueAt(keyIndex); //the page or first page greater

            // Move to child page
            currentPage = pageTable.get(childPageId);
            if (currentPage == null) {
                throw new RuntimeException("Child page not found in page table");
            }
        }
        return (LeafPage<Key, RID>) currentPage;
    }

    private void insertIntoParent(BPlusTreePage leftChild, Key midkey, BPlusTreePage rightChild, Stack<BPlusTreePage> parentStack) {
        int leftPageId = leftChild.getPageID();
        int rightPageId = rightChild.getPageID();

        // Case 1: creates new root
        if (parentStack.isEmpty()) {
            InternalPage<Key> newRoot = new InternalPage<>();
            int newRootId = allocatePageId();
            newRoot.init(leftChild.getMaxSize(), newRootId);

            newRoot.setKeyAt(1, midkey); // first key is dummy, real keys start at 1
            newRoot.setValueAt(0, leftPageId);
            newRoot.setValueAt(1, rightPageId);
            newRoot.setSize(1); // size = # of valid keys = 1

            pageTable.put(newRootId, newRoot);
            rootPageId = newRootId;
            return;
        }

        // Case 2: Insert midkey into existing parent
        InternalPage<Key> parent = (InternalPage<Key>) parentStack.pop();

        // Find index of leftChild in parent
        if(parent.valueIndex(leftPageId)==-1) //not found
        {
            System.out.println("[Insert Into Parent]: pageID nit Found in the paernt");
            return;
        }

        // Insert entry
        if (parent.insertEntry(midkey, rightPageId)) {
            Set<Integer> seen = new HashSet<>();
            int [] pageIdArray = parent.getPageIDs();
            for (int i = 0; i <= parent.getSize(); i++) {
                if (!seen.add(pageIdArray[i])) {
                    throw new RuntimeException("❌ Duplicate child pointer detected in internal node!");
                }
            }
            return; // No overflow, done
        }

        // Overflow: split parent
        InternalPage<Key> newParent = new InternalPage<>();
        int newParentId = allocatePageId();
        newParent.init(parent.getMaxSize(), newParentId);

        // Split current parent into two internal pages
        Key newMidKey = parent.splitInternalPage(newParent);

        pageTable.put(newParentId, newParent);

        // Recurse upward
        insertIntoParent(parent, newMidKey, newParent, parentStack);
    }


    public RID getValue(Key key)
    {
        LeafPage<Key, RID> leaf = findLeafPage(key, null);  // No need to track parents for search
       // System.out.println("[getValue]: We Search for "+key+" in PageID: "+leaf.getPageID());
        int size = leaf.getSize();
        for (int i = 0; i < size; i++) {
            if (key.compareTo(leaf.keyAt(i)) == 0) {
                return leaf.valueAt(i);
            }
        }
        return null;  // Key not found
    }

    public boolean remove(Key key) {
        Stack<BPlusTreePage> parentStack = new Stack<>();
        LeafPage<Key, RID> leaf = findLeafPage(key, parentStack);

        boolean removed = leaf.remove(key);
        if (!removed) return false;

        if(leaf.getSize() < ceil(leaf.getMaxSize()/2))
            handleUnderflow(leaf, parentStack);
        return true;
    }

    private void handleUnderflow(BPlusTreePage page, Stack<BPlusTreePage> stack) {
        // Underflow at root is allowed if it still has one child or entry
        if (stack.isEmpty()) return;

        BPlusTreePage parentPage = stack.pop();
        InternalPage<Key> parent = (InternalPage<Key>) parentPage;

        int indexInParent = parent.valueIndex(page.getPageID());
        if (indexInParent == -1)
            throw new RuntimeException("❌ Page not found in parent during underflow.");

        // ---- Try right sibling
        if (indexInParent + 1 <= parent.getSize()) {
            int rightSiblingId = parent.valueAt(indexInParent + 1);
            BPlusTreePage rightSibling = pageTable.get(rightSiblingId);

            if (rightSibling != null && rightSibling.getSize() > (int) Math.ceil(rightSibling.getMaxSize() / 2.0)) {
                if (page.isLeafPage())
                    redistributeLeaf((LeafPage<Key, RID>) page, (LeafPage<Key, RID>) rightSibling, parent, indexInParent);
                else
                    redistributeInternal((InternalPage<Key>) page, (InternalPage<Key>) rightSibling, parent, indexInParent);
                return;
            }
        }

        // ---- Try left sibling
        if (indexInParent - 1 >= 0) {
            int leftSiblingId = parent.valueAt(indexInParent - 1);
            BPlusTreePage leftSibling = pageTable.get(leftSiblingId);

            if (leftSibling != null && leftSibling.getSize() > (int) Math.ceil(leftSibling.getMaxSize() / 2.0)) {
                if (page.isLeafPage())
                    redistributeLeaf((LeafPage<Key, RID>) leftSibling, (LeafPage<Key, RID>) page, parent, indexInParent - 1);
                else
                    redistributeInternal((InternalPage<Key>) leftSibling, (InternalPage<Key>) page, parent, indexInParent - 1);
                return;
            }
        }

        // ---- If redistribution failed → merge
        if (indexInParent + 1 <= parent.getSize()) {
            // merge with right sibling
            int rightSiblingId = parent.valueAt(indexInParent + 1);
            BPlusTreePage rightSibling = pageTable.get(rightSiblingId);

            if (page.isLeafPage())
                mergeLeafPages((LeafPage<Key, RID>) page, (LeafPage<Key, RID>) rightSibling, parent, stack);
            else
                mergeInternalPages((InternalPage<Key>) page, (InternalPage<Key>) rightSibling, parent, indexInParent);
        } else if (indexInParent - 1 >= 0) {
            // merge with left sibling
            int leftSiblingId = parent.valueAt(indexInParent - 1);
            BPlusTreePage leftSibling = pageTable.get(leftSiblingId);

            if (page.isLeafPage())
                mergeLeafPages((LeafPage<Key, RID>) leftSibling, (LeafPage<Key, RID>) page, parent, stack);
            else
                mergeInternalPages((InternalPage<Key>) leftSibling, (InternalPage<Key>) page, parent, indexInParent - 1);
        }

        // Recursive upward if parent now underflows
        if (parent.getSize() < Math.ceil(parent.getMaxSize() / 2.0)) {
            handleUnderflow(parent, stack);
        }
    }


    private void redistributeLeaf(LeafPage<Key, RID> underflowedLeaf,
                     LeafPage<Key, RID> siblingLeaf,
                     InternalPage<Key> parent,
                     int leafIndexInParent)
    {
        boolean isLeftSibling;
        if(siblingLeaf.keyAt(0).compareTo(underflowedLeaf.keyAt(0)) > 0) // Reliable check
            isLeftSibling = false ;
        else
            isLeftSibling = true ;

        if (isLeftSibling) {
            // Borrow from left sibling: move last key of sibling to front of leaf
            int sibLast = siblingLeaf.getSize() - 1;
            Key borrowedKey = siblingLeaf.keyAt(sibLast);
            RID borrowedVal = siblingLeaf.valueAt(sibLast);
            underflowedLeaf.insertIntoLeaf(borrowedKey, borrowedVal);

            // Shrink sibling
            siblingLeaf.setSize(siblingLeaf.getSize() - 1);

            // Update separator key in parent
            parent.setKeyAt(leafIndexInParent, borrowedKey);

        } else {
            // Borrow from right sibling: move first key of sibling to end of leaf
            Key borrowedKey = siblingLeaf.keyAt(0);
            RID borrowedVal = siblingLeaf.valueAt(0);

            // Append to leaf
            int leafSize = underflowedLeaf.getSize();
            underflowedLeaf.insertIntoLeaf(borrowedKey, borrowedVal);

            // Shift sibling contents left
            for (int i = 0; i < siblingLeaf.getSize()-1; i++) {
                siblingLeaf.setKeyAt(i, siblingLeaf.keyAt(i + 1));
                siblingLeaf.setValueAt(i, siblingLeaf.valueAt(i + 1));
            }
            siblingLeaf.setSize(siblingLeaf.getSize() - 1);

            // Update separator key in parent
            parent.setKeyAt(leafIndexInParent + 1, siblingLeaf.keyAt(0));
        }
    }

    private void mergeLeafPages (LeafPage<Key, RID> underflowedLeaf,
                                 LeafPage<Key, RID> siblingLeaf,
                                 InternalPage<Key> parent,
                                 Stack<BPlusTreePage> stack)
    {

        boolean isLeftSibling;
        if(siblingLeaf.keyAt(0).compareTo(underflowedLeaf.keyAt(0)) < 0) // Reliable check
            isLeftSibling = true ;
        else
            isLeftSibling = false ;

        LeafPage<Key, RID> leftLeaf;
        LeafPage<Key, RID> rightLeaf;

        if (isLeftSibling)
        {
            leftLeaf = siblingLeaf;
            rightLeaf = underflowedLeaf;
        }
        else
        {
            leftLeaf = underflowedLeaf;
            rightLeaf = siblingLeaf;
        }

        int offset = leftLeaf.getSize();
        // 1. Copying
        //System.out.println("[inside Merge Leaf]: the size of the left: "+offset+" the size of the right: "+rightLeaf.getSize());
        for (int i = 0; i < rightLeaf.getSize(); i++) {
            leftLeaf.setKeyAt(offset + i, rightLeaf.keyAt(i));
            leftLeaf.setValueAt(offset + i, rightLeaf.valueAt(i));
        }

        leftLeaf.setSize(leftLeaf.getSize() + rightLeaf.getSize());
        leftLeaf.setNextPageId(rightLeaf.getNextPageId());
        pageTable.remove(rightLeaf.getPageID());
        int leftPageID = leftLeaf.getPageID();
        int leftIndexInParent = parent.valueIndex(leftPageID);
        System.out.println("[Merge Leaf]: the leftIndexInParent: "+leftIndexInParent+ " and size: "+parent.getSize());
        if(!parent.removeEntry(leftIndexInParent + 1)) // Key and right pageID
        {
            throw new RuntimeException("❌ Failed while removing the Key from the parent");
        }

        //    ===========================   recursive part  ======================
        if (parent.getSize() < Math.ceil(parent.getMaxSize() / 2.0)) {
            handleUnderflow(parent, stack);
        }

        // Special case: if parent becomes empty and is root
        if (parent == pageTable.get(rootPageId) && parent.getSize() == 0) {
            rootPageId = leftLeaf.getPageID(); // Tree shrinks to only one node
        }
    }

    private void redistributeInternal(InternalPage<Key> underflowedPage,
                                      InternalPage<Key> siblingPage,
                                      InternalPage<Key> parent,
                                      int underflowIndex) {
        boolean isLeftSibling = siblingPage.keyAt(0).compareTo(underflowedPage.keyAt(0)) < 0;

        if (isLeftSibling) {
            // Borrow the last key and child from the left sibling
            int sibSize = siblingPage.getSize();

            // Shift underflowedPage to make space at front
            for (int i = underflowedPage.getSize(); i > 0; i--) {
                underflowedPage.setKeyAt(i, underflowedPage.keyAt(i - 1));
                underflowedPage.setValueAt(i + 1, underflowedPage.valueAt(i));
            }
            underflowedPage.setValueAt(1, underflowedPage.valueAt(0));

            // Move separator key from parent into underflowed page
            Key parentSeparator = parent.keyAt(underflowIndex);
            underflowedPage.setKeyAt(0, parentSeparator);
            underflowedPage.setValueAt(0, siblingPage.valueAt(sibSize)); // rightmost child of sibling

            // Replace parent key with sibling's last key
            Key borrowedKey = siblingPage.keyAt(sibSize - 1);
            parent.setKeyAt(underflowIndex, borrowedKey);

            siblingPage.setSize(sibSize - 1);
            underflowedPage.setSize(underflowedPage.getSize() + 1);

        } else {
            // Borrow first key and child from right sibling
            int sibSize = siblingPage.getSize();

            // Move parent's separator key down into underflowed page
            Key parentSeparator = parent.keyAt(underflowIndex + 1);
            underflowedPage.setKeyAt(underflowedPage.getSize(), parentSeparator);
            underflowedPage.setValueAt(underflowedPage.getSize() + 1, siblingPage.valueAt(0));

            // Replace parent's separator key with sibling's first key
            Key newSeparator = siblingPage.keyAt(0);
            parent.setKeyAt(underflowIndex + 1, newSeparator);

            // Shift sibling left
            for (int i = 0; i < sibSize - 1; i++) {
                siblingPage.setKeyAt(i, siblingPage.keyAt(i + 1));
                siblingPage.setValueAt(i, siblingPage.valueAt(i + 1));
            }
            siblingPage.setValueAt(sibSize - 1, siblingPage.valueAt(sibSize));

            siblingPage.setSize(sibSize - 1);
            underflowedPage.setSize(underflowedPage.getSize() + 1);
        }
    }
    private void mergeInternalPages(InternalPage<Key> left,
                                    InternalPage<Key> right,
                                    InternalPage<Key> parent,
                                    int separatorIndexInParent) {
        Key separatorKey = parent.keyAt(separatorIndexInParent);

        int leftSize = left.getSize();

        // Append the separator key to the left node
        left.setKeyAt(leftSize, separatorKey);
        left.setValueAt(leftSize + 1, right.valueAt(0));
        left.setSize(leftSize + 1);

        // Copy keys and values from right into left
        for (int i = 0; i < right.getSize(); i++) {
            left.setKeyAt(left.getSize(), right.keyAt(i));
            left.setValueAt(left.getSize(), right.valueAt(i + 1));
            left.setSize(left.getSize() + 1);
        }

        // Remove the right page from the page table
        pageTable.remove(right.getPageID());

        // Remove the separator key and right child from parent
        int parentSize = parent.getSize();
        for (int i = separatorIndexInParent; i < parentSize - 1; i++) {
            parent.setKeyAt(i, parent.keyAt(i + 1));
            parent.setValueAt(i + 1, parent.valueAt(i + 2));
        }
        parent.setKeyAt(parentSize - 1, null);
        parent.setValueAt(parentSize, 0);
        parent.setSize(parentSize - 1);

        // Check if parent now underflows
        if (parent.getSize() < Math.ceil(parent.getMaxSize() / 2.0) && parent.getPageID() != rootPageId) {
            handleUnderflow(parent, null);  // Or pass stack if you need recursion
        }

        // If parent becomes empty and is root, update root
        if (parent.getPageID() == rootPageId && parent.getSize() == 0) {
            rootPageId = left.getPageID();  // left becomes new root
        }
    }


    public List<Key> getAllKeysInOrder() {
        List<Key> result = new ArrayList<>();
        int pageId = rootPageId;

        // Go to the leftmost leaf
        while (!pageTable.get(pageId).isLeafPage()) {
            pageId = ((InternalPage<Key>) pageTable.get(pageId)).valueAt(0);
        }

        // Iterate through linked list of leaf pages
        Set<Integer> visitedPages = new HashSet<>();  // Prevent infinite loops
        while (pageId != -1) {
            if (visitedPages.contains(pageId)) {
                throw new RuntimeException("Cycle detected in leaf page links!");
            }
            visitedPages.add(pageId);

            LeafPage<Key, RID> leaf = (LeafPage<Key, RID>) pageTable.get(pageId);
            for (int i = 0; i < leaf.getSize(); i++) {
                result.add(leaf.keyAt(i));
            }
            pageId = leaf.getNextPageId();
        }
        return result;
    }


    // =======================================    Print     =========================================
    public void printTree() {
        System.out.println("\n=== B+ TREE STRUCTURE ===");

        BPlusTreePage root = pageTable.get(rootPageId);
        if (!root.isLeafPage()) {
            printInternalPage((InternalPage<Key>) root, 0);
        } else{
            printLeafPage((LeafPage<Key, RID>) root, 0);
        }

        System.out.println("==========================\n");
    }
    private void printInternalPage(InternalPage<Key> page, int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + "Internal Page ID " + page.getPageID());
        System.out.println(indent + "  Keys:    " + Arrays.toString(page.getKeys()));
        System.out.println(indent + "  Children:" + Arrays.toString(page.getPageIDs()));

        for (int i = 0; i <= page.getSize(); i++) {
            int childId = page.valueAt(i);
            BPlusTreePage child = pageTable.get(childId);

            if (!child.isLeafPage()) {
                printInternalPage((InternalPage<Key>) child, level + 1);
            } else{
                printLeafPage((LeafPage<Key, RID>) child, level + 1);
            }
        }
    }
    private void printLeafPage(LeafPage<Key, RID> page, int level) {
        String indent = "  ".repeat(level);
        System.out.print(indent + "Leaf Page ID " + page.getPageID() + " [ ");
        for (int i = 0; i < page.getSize(); i++) {
            System.out.print(page.keyAt(i) + ":" + page.valueAt(i) + " ");
        }
        System.out.println("] → Next: " + page.getNextPageId());
    }



}