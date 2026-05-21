# Virtual Memory Page Replacement Algorithms — UR_OS Simulator Report

## Overview

UR_OS is a Java-based operating system simulator that supports multiple memory management strategies (Contiguous, Paging, Segmentation). This report documents the implementation of **five page replacement (victim selection) algorithms** for the virtual memory paging subsystem.

---

## Virtual Memory Architecture

### Key Components

| Component | Description |
|---|---|
| **`PMM_Paging`** | Process Memory Manager for paging — maintains the process page table (physical frames) and virtual page table (swap frames) |
| **`SMM_Paging`** | System Memory Manager — handles logical-to-physical address translation and triggers page fault handling |
| **`PageTable`** | Collection of `PageTableEntry` objects, one per virtual page |
| **`PageTableEntry`** | Per-page metadata: frame ID, valid bit, dirty bit, referenced bit, clock (load timestamp) |
| **`SwapMemory`** | Byte-array-backed swap space for storing evicted pages |
| **`ProcessVirtualMemoryManager`** | Abstract base class for victim selection algorithms |

### Page Fault Handling Flow

```
Process accesses logical address
        │
        ▼
SMM_Paging.getPhysicalAddress()
        │
        ├─── HIT: PageTableEntry.valid == true
        │         → Set referenced bit, mark dirty if STORE
        │         → Return physical address
        │
        └─── MISS: PageTableEntry.valid == false
                  │
                  ▼
            PMM_Paging.getVictim()
                  │
                  ├─── loadedPages < assignedPages → return -1 (allocate new free frame)
                  │
                  └─── loadedPages == assignedPages → pvmm.getVictim() selects victim
                        │
                        ├─── If victim page is dirty → STORE_PAGE (write to swap)
                        └─── LOAD_PAGE (bring needed page from swap into victim's frame)
```

### Configuration

- **Memory:** 12,289 bytes (12 KB)
- **Swap:** 65,536 bytes (64 KB)
- **Page size:** 64 bytes
- **Frames per process (VM mode):** 3
- **Processes:** 6, each accessing 4–5 distinct pages, forcing page faults and victim selection

---

## Simulation Scenario Details

### Global System Parameters

| Parameter | Value |
|---|---|
| Physical memory | 12,289 bytes (12 KB) |
| Swap memory | 65,536 bytes (64 KB) |
| Page size | 64 bytes |
| Frames per process (VM limit) | 3 |
| Memory manager type | Paging |
| Page replacement algorithm | Configurable (LRU default) |
| Max simulation cycles | 1,000 |
| Actual cycles to completion | 97 |

### Process Workload Specification

Six processes arrive at staggered times. Each process is allocated **3 frames maximum**, but performs memory accesses across **4–5 distinct pages**, deliberately exceeding its frame allocation to trigger page faults and exercise the victim selection pipeline.

#### Page Addressing Reference

Logical address `A` maps to:
- **Page number** = `A / 64` (integer division)
- **Offset within page** = `A % 64`

#### Process Timeline

```
Clock  ──►
0      3              10             30       48       68
|──────|──────────────|──────────────|────────|────────|────────
P0     P1             P2             P3       P4       P5
arrives arrives       arrives        arrives  arrives  arrives
(2048B) (1024B)       (3072B)        (1536B)  (2048B)  (768B)
```

#### Per-Process Memory Access Trace

**P0** — arrival 0, size 2048 bytes, **32 pages** (0–31)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | LOAD | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | LOAD | 128 | 2 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | LOAD | 256 | 4 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | LOAD | 512 | 8 | 🔄 **victim evicted** | 4th page → triggers replacement |

**Frames at victim selection:** {0, 2, 4} → one must be replaced for page 8.

---

**P1** — arrival 3, size 1024 bytes, **16 pages** (0–15)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | STORE 0→byte[42] | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | STORE 64→byte[42] | 64 | 1 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | STORE 192→byte[42] | 192 | 3 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | STORE 448→byte[42] | 448 | 7 | 🔄 **victim evicted** | 4th page → triggers replacement |

**Frames at victim selection:** {0, 1, 3} → one must be replaced for page 7.
All accesses are STORE, so the dirty bit is set on every accessed page.

---

**P2** — arrival 10, size 3072 bytes, **48 pages** (0–47)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | LOAD | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | LOAD | 320 | 5 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | LOAD | 640 | 10 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | LOAD | 1280 | 20 | 🔄 **1st victim evicted** | 4th page → triggers replacement |
| 5 | LOAD | 1920 | 30 | 🔄 **2nd victim evicted** | 5th page → triggers replacement again |

**Frames at victim selection:** {0, 5, 10} → victim picked for page 20, then another for page 30.
This is the **heaviest paging process** — 2 separate victim selections required.

---

**P3** — arrival 30, size 1536 bytes, **24 pages** (0–23)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | STORE 0→byte[99] | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | STORE 192→byte[99] | 192 | 3 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | STORE 384→byte[99] | 384 | 6 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | STORE 768→byte[99] | 768 | 12 | 🔄 **victim evicted** | 4th page → triggers replacement |

**Frames at victim selection:** {0, 3, 6} → one must be replaced for page 12.
All accesses are STORE, so all pages become dirty.

---

**P4** — arrival 48, size 2048 bytes, **32 pages** (0–31)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | LOAD | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | LOAD | 256 | 4 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | LOAD | 512 | 8 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | LOAD | 1024 | 16 | 🔄 **victim evicted** | 4th page → triggers replacement |

**Frames at victim selection:** {0, 4, 8} → one must be replaced for page 16.

---

**P5** — arrival 68, size 768 bytes, **12 pages** (0–11)

| # | Instruction | Logical Addr | Page | Frame State | Notes |
|---|---|---|---|---|---|
| 1 | STORE 0→byte[77] | 0 | 0 | ✅ pre-loaded at creation | page 0 always resident initially |
| 2 | STORE 128→byte[77] | 128 | 2 | ⬇ fill frame #2 | 2nd page in, no victim needed |
| 3 | STORE 320→byte[77] | 320 | 5 | ⬇ fill frame #3 | 3rd page in, frames now full (3/3) |
| 4 | STORE 512→byte[77] | 512 | 8 | 🔄 **victim evicted** | 4th page → triggers replacement |

**Frames at victim selection:** {0, 2, 5} → one must be replaced for page 8.
All accesses are STORE.

---

### Scenario Summary Table

| Process | Arrival | Size (B) | Total Pages | Distinct Pages Accessed | Page Faults (no victim) | Victim Selections | Store or Load |
|---|---|---|---|---|---|---|---|
| P0 | 0 | 2,048 | 32 | 4 (0, 2, 4, 8) | 2 | 1 | LOAD |
| P1 | 3 | 1,024 | 16 | 4 (0, 1, 3, 7) | 2 | 1 | STORE |
| P2 | 10 | 3,072 | 48 | 5 (0, 5, 10, 20, 30) | 2 | 2 | LOAD |
| P3 | 30 | 1,536 | 24 | 4 (0, 3, 6, 12) | 2 | 1 | STORE |
| P4 | 48 | 2,048 | 32 | 4 (0, 4, 8, 16) | 2 | 1 | LOAD |
| P5 | 68 | 768 | 12 | 4 (0, 2, 5, 8) | 2 | 1 | STORE |
| **Total** | | **10,496** | **164** | **25 distinct** | **12** | **7** | — |

### Why This Scenario Exercises the Algorithms

1. **Every process exceeds its frame allocation** (4–5 pages accessed, only 3 frames available), guaranteeing at least one victim selection per process.
2. **P2 forces two victim selections** (5 distinct pages), allowing evaluation of how the algorithm behaves under repeated pressure.
3. **Mix of LOAD and STORE** operations — STORE sets the dirty bit, which affects whether a victim page must be written back to swap before eviction.
4. **Staggered arrivals** (clocks 0, 3, 10, 30, 48, 68) create interleaved execution, so different processes compete for the global free frame pool before VM limits kick in.
5. **Non-sequential page access patterns** — pages are accessed at widely spaced intervals (e.g., pages 0, 5, 10, 20, 30 for P2), preventing trivial LRU behavior and stressing the replacement policy.

---

## Algorithm 1: FIFO (First-In, First-Out)

**File:** `PVMM_FIFO.java`

### Strategy

Selects the page that has been resident in memory the longest. Each `PageTableEntry` stores a `clock` value set by the OS at the moment the page is loaded into a frame. The victim is the valid page with the **minimum clock value**.

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    int victim = -1;
    int oldestClock = Integer.MAX_VALUE;
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) {
            if (pte.getClock() < oldestClock) {
                oldestClock = pte.getClock();
                victim = i;
            }
        }
        i++;
    }
    return victim;
}
```

### Complexity

- **Time:** O(n) where n = number of page table entries
- **Space:** O(1) auxiliary

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Simple, fair, no need to track access patterns |
| **Cons** | Suffers from **Belady's Anomaly** — increasing frames can increase page faults; may evict heavily-used pages |
| **Best for** | Baseline comparison; workloads with uniform page access |

---

## Algorithm 2: LRU (Least Recently Used)

**File:** `PVMM_LRU.java` (pre-existing, only working algorithm before this assignment)

### Strategy

Selects the page that has **not been accessed for the longest time**. The algorithm walks the `memoryAccesses` history list **backwards** (most recent → oldest), collecting unique valid page numbers. The last page added to the collection is the least recently used.

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    LinkedList<Integer> pages = new LinkedList();
    LinkedList<Integer> validListPages = new LinkedList();
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) validListPages.add(i);
        i++;
    }
    int size = memoryAccesses.size() - 1;
    while (size >= 0 && pages.size() < validListPages.size()) {
        int temp = memoryAccesses.get(size);
        if (!pages.contains(temp) && validListPages.contains(temp)) {
            pages.add(temp);
        }
        size--;
    }
    return pages.getLast(); // Least recently used
}
```

### Complexity

- **Time:** O(n × m) where n = number of memory accesses, m = number of valid pages (contains-check on LinkedList is O(m))
- **Space:** O(m) for the pages collection

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Excellent approximation of optimal; exploits temporal locality |
| **Cons** | Requires maintaining full access history; O(n×m) lookup per page fault |
| **Best for** | General-purpose workloads with strong temporal locality |

---

## Algorithm 3: LFU (Least Frequently Used)

**File:** `PVMM_LFU.java`

### Strategy

Counts how many times each valid page has been accessed in the `memoryAccesses` history. The page with the **lowest access count** is selected as the victim. The intuition is that rarely-used pages are less likely to be needed soon.

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    LinkedList<Integer> validPages = new LinkedList<>();
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) validPages.add(i);
        i++;
    }

    int minCount = Integer.MAX_VALUE;
    int victim = validPages.get(0);

    for (int page : validPages) {
        int count = 0;
        for (int access : memoryAccesses) {
            if (access == page) count++;
        }
        if (count < minCount) {
            minCount = count;
            victim = page;
        }
    }
    return victim;
}
```

### Complexity

- **Time:** O(m × n) where m = valid pages, n = access history length
- **Space:** O(m) for the valid pages list

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Retains "popular" pages; good for stable access patterns |
| **Cons** | Penalizes pages with early burst of accesses that later go cold; doesn't adapt quickly to changing patterns |
| **Best for** | Workloads with stable, predictable hot pages |

---

## Algorithm 4: MFU (Most Frequently Used)

**File:** `PVMM_MFU.java`

### Strategy

The inverse of LFU. Selects the page with the **highest access count** as the victim. The counterintuitive reasoning: a page that was accessed many times was likely brought in for a specific task that is now complete, so it is unlikely to be needed again soon.

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    LinkedList<Integer> validPages = new LinkedList<>();
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) validPages.add(i);
        i++;
    }

    int maxCount = -1;
    int victim = validPages.get(0);

    for (int page : validPages) {
        int count = 0;
        for (int access : memoryAccesses) {
            if (access == page) count++;
        }
        if (count > maxCount) {
            maxCount = count;
            victim = page;
        }
    }
    return victim;
}
```

### Complexity

- **Time:** O(m × n) — identical to LFU
- **Space:** O(m)

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Useful for "scan once" workloads where heavily-used pages are unlikely to be revisited |
| **Cons** | Poor performance for general workloads; evicts the very pages the program relies on most |
| **Best for** | Specialized sequential/one-pass workloads |

---

## Algorithm 5: Clock (Second Chance) — *Custom*

**File:** `PVMM_Clock.java`

### Strategy

An approximation of LRU that uses a **reference bit** per page. The algorithm scans valid pages in page-table order:

1. If a page's `referenced` bit is **false** → select it as victim immediately
2. If a page's `referenced` bit is **true** → clear it and continue (the page gets a "second chance")
3. If all pages have `referenced == true` → clear all bits and select the first valid page

The `referenced` bit is set to `true` every time a page is successfully accessed (on a page table hit).

### Additional Infrastructure

A `referenced` boolean field was added to `PageTableEntry`, with getter/setter methods. The bit is set during address translation in `PMM_Paging.getFrameMemoryAddressFromLogicalMemoryAddress()`:

```java
this.addMemoryAccess(page);
pt.getList().get(page).setReferenced(true);
```

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    LinkedList<Integer> validIndices = new LinkedList<>();
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) validIndices.add(i);
        i++;
    }

    // First pass: find first unreferenced valid page
    for (int idx : validIndices) {
        if (!pt.getList().get(idx).isReferenced()) {
            return idx;
        }
    }

    // Second pass: clear all reference bits and return first valid
    for (int idx : validIndices) {
        pt.getList().get(idx).setReferenced(false);
    }
    return validIndices.get(0);
}
```

### Complexity

- **Time:** O(n) worst case (two passes over valid pages)
- **Space:** O(m) for the valid indices list

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Hardware-friendly (single reference bit); approximates LRU with O(n) cost; no access history needed |
| **Cons** | Scan order bias (page 0 always checked first); simplified version lacks a rotating "hand" pointer |
| **Best for** | Real systems — this is the algorithm used in many production OS kernels |

---

## Algorithm 6: Random — *Custom*

**File:** `PVMM_Random.java`

### Strategy

Selects a **uniformly random** valid page as the victim. No analysis of access patterns, timestamps, or usage frequency is performed.

### Implementation

```java
public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
    LinkedList<Integer> validPages = new LinkedList<>();
    int i = 0;
    for (PageTableEntry pte : pt.getList()) {
        if (pte.isValid()) validPages.add(i);
        i++;
    }
    if (validPages.isEmpty()) return -1;
    return validPages.get(r.nextInt(validPages.size()));
}
```

### Complexity

- **Time:** O(n) to collect valid pages, O(1) for selection
- **Space:** O(m) for the valid pages list

### Characteristics

| Aspect | Detail |
|---|---|
| **Pros** | Zero overhead for tracking; immune to pathological access patterns; simplest to implement |
| **Cons** | Ignores all available information; may evict heavily-used pages purely by chance |
| **Best for** | Baseline comparison; when tracking metadata is too expensive |

---

## Algorithm Comparison Summary

| Algorithm | Time Complexity | Metadata Needed | Eviction Policy | Belady's Anomaly? |
|---|---|---|---|---|
| **FIFO** | O(n) | Load timestamp (clock) | Oldest loaded | ✅ Yes |
| **LRU** | O(n × m) | Full access history | Least recently accessed | ❌ No |
| **LFU** | O(n × m) | Full access history | Least frequently accessed | ❌ No |
| **MFU** | O(n × m) | Full access history | Most frequently accessed | ❌ No |
| **Clock** | O(n) | Reference bit (per page) | First unreferenced | ✅ Yes (approximate) |
| **Random** | O(n) | None | Uniform random | N/A |

Where **n** = number of page table entries, **m** = number of valid (in-memory) pages.

---

## Infrastructure Fixes Applied

Beyond implementing the algorithms, the following bugs and configuration issues were resolved to make virtual memory functional:

1. **`SMM` switched from `SEGMENTATION` to `PAGING`** — The VM pipeline only works with paging
2. **`VIRTUAL_MEMORY_MODE_ON` enabled** — Previously `false`, meaning all pages were loaded at process creation with zero page faults
3. **`SWAP_MEMORY_SIZE` corrected** — Was 1 GB (impractical for byte-array swap); reduced to 64 KB
4. **`SwapMemory` constructor fixed** — Was initialized with `MEMORY_SIZE` (12 KB) instead of `SWAP_MEMORY_SIZE` in both `SystemOS` and `MemoryUnit`
5. **`getVFrameMemoryAddressFromLogicalMemoryAddress(int)` bug fixed** — Was returning a byte offset (`vframe × PAGE_SIZE`) instead of a frame ID, causing `ArrayIndexOutOfBoundsException` in swap memory access
6. **Test processes redesigned** — Each process now accesses 4–5 distinct pages (with only 3 frames allocated), forcing page faults and victim selection on every process
7. **`referenced` bit added to `PageTableEntry`** — Required for the Clock algorithm; also updated `toString()` to display it

---

## How to Switch Algorithms

Change the `PVMM` constant in `OS.java`:

```java
public static final ProcessVirtualMemoryManagerType PVMM = ProcessVirtualMemoryManagerType.LRU;
// Options: FIFO, LRU, LFU, MFU, CLOCK, RANDOM
```

Recompile and run:
```bash
javac ur_os/UR_OS.java
java ur_os.UR_OS
```
