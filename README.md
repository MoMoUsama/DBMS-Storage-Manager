# Storage Manager for a Database Management System (DBMS)

This project implements a **Storage Manager** for a **Database Management System**, handling page-based disk operations, page buffering, and indexing using a **B+ Tree** data structure. The system is designed for **performance and modularity**, with support for **multithreaded disk scheduling** and efficient **buffer pool management**.

---

## Project Components

### 1. B+ Tree Indexing
- Implements a **B+ Tree** for efficient storage and retrieval of key-value pairs.
- Supports key operations: 
  - `insert(Key, Value)`
  - `search(Key)`
  - `delete(Key)`
- Ensures optimal tree balance using:
  - **Redistribution**
  - **Merging**
  - **Leaf and Internal node splitting**
- Allows **sorted key traversal** through linked leaf nodes.

---

### 2. Page Layout

Pages are categorized into two types **Internal Pages** and **Leaf Pages**, Both page types contain headers with metadata such as page type, current size, next/prev pointers (for leaves), etc.:
- **Internal Pages**:
  - Store ordered keys and child page pointers.
  - structure:
    ```
    ┌────────────┬──────────────┬───────────────┐
    │ Page Header│   Keys[]     │ PageIDs[]     │
    └────────────┴──────────────┴───────────────┘
    ```

#### 2.1 Internal Page Layout

| Offset | Size (bytes) | Field                                      |
|--------|--------------|--------------------------------------------|
| 0      | 4            | `pageType` (e.g., 2 for internal)          |
| 4      | 4            | `pageID`                                   |
| 8      | 4            | `size` (number of child pointers - 1)      |
| 12     | 4            | `maxSize` (maximum number of entries)      |
| 16     | N            | Alternating pattern:<br>`childID_0, key_0, childID_1, ..., key_n` |


**Leaf Pages**
- Store ordered keys and associated **record identifiers (RIDs)**.
- Structure:
    ```
    ┌────────────┬──────────────┬───────────────┐
    │ Page Header│   Keys[]     │ RIDs[]        │
    └────────────┴──────────────┴───────────────┘
    ```
  

#### 2.2 Leaf Page Layout

| Offset | Size (bytes) | Field                                     |
|--------|--------------|-------------------------------------------|
| 0      | 4            | `pageType` (e.g., 1 for leaf)             |
| 4      | 4            | `pageID`                                  |
| 8      | 4            | `nextPageID`                              |
| 12     | 4            | `size` (number of key-value pairs)        |
| 16     | 4            | `maxSize` (maximum number of pairs)       |
| 20     | N            | Actual `key-value` pairs                  |


**Notes**

- All fields **before the key area** are part of the **page header (metadata)**.
- Internal pages follow a **child-key-child** pattern.
- Leaf pages use **sequential key-RID** pairs.
- This structured layout ensures compatibility with fixed-size pages and efficient memory/disk operations.


---

### 3. Buffer Pool Manager
- Manages **in-memory frames** that hold pages from disk.
- Handles:
  - **Page loading** from disk on demand.
  - **Page replacement** policies when the pool is full.
  - **Pinning/unpinning** logic to prevent eviction of in-use pages.
- Reduces direct disk I/O and improves performance.

---

### 4. Multithreaded Disk Scheduler
- Background thread processes **asynchronous disk I/O requests** from a queue.
- Supports:
  - **Page fetch** from disk
  - **Page flush** to disk
- Reduces blocking of main execution threads by offloading I/O.

---

### 5. Page I/O Management
- All pages are persisted on disk using a fixed page size (e.g., 4 KB).
- Supports:
  - `readPage(pageId)` → Load page into buffer.
  - `writePage(page)` → Flush page to disk.
---

## Key Features
- Efficient indexing with B+ Tree
- Dynamic page splitting/merging
- In-memory buffer management
- Background disk I/O scheduler
- Concurrent-safe operations

---

## Example Use Case
This system can be used as a **backend storage engine** for educational or lightweight database systems, especially those requiring **indexing and buffering strategies** close to real DBMS internals like PostgreSQL or SQLite.

---

## Folder Structure (Example)
