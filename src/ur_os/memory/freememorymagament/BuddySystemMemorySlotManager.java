package ur_os.memory.freememorymagament;
 
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
 
/**
 * Buddy System free memory manager.
 *
 * Memory is split into power-of-2 sized blocks. On allocation, a block
 * is recursively halved until it fits the request. On free, a block is
 * merged with its buddy if the buddy is also free.
 *
 * Constraint: total memory size is rounded up to the nearest power of 2.
 */
public class BuddySystemMemorySlotManager extends FreeMemorySlotManager {
 
    private final Map<Integer, LinkedList<MemorySlot>> freeLists;
    private final int totalSize;
    private final int maxOrder;
 
    public BuddySystemMemorySlotManager(int memSize) {
        super(memSize);
        this.totalSize = nextPowerOfTwo(memSize);
        this.maxOrder = log2(this.totalSize);
        this.freeLists = new HashMap<>();
 
        for (int k = 0; k <= maxOrder; k++) {
            freeLists.put(k, new LinkedList<>());
        }
        freeLists.get(maxOrder).add(new MemorySlot(0, this.totalSize));
 
        this.list.clear();
        this.list.add(new MemorySlot(0, this.totalSize));
    }
 
    // ------------------------------------------------------------------
    // Allocation
    // ------------------------------------------------------------------
 
    @Override
    public MemorySlot getSlot(int size) {
        if (size <= 0) return null;
 
        int order = orderFor(size);
        if (order > maxOrder) {
            System.out.println("Buddy System - Requested size " + size + " exceeds total memory.");
            return null;
        }
 
        int availableOrder = -1;
        for (int k = order; k <= maxOrder; k++) {
            if (!freeLists.get(k).isEmpty()) {
                availableOrder = k;
                break;
            }
        }
 
        if (availableOrder == -1) {
            System.out.println("Buddy System - Not enough memory for request of size " + size);
            return null;
        }
 
        while (availableOrder > order) {
            MemorySlot block = freeLists.get(availableOrder).removeFirst();
            int halfSize = block.getSize() / 2;
            MemorySlot left  = new MemorySlot(block.getBase(), halfSize);
            MemorySlot right = new MemorySlot(block.getBase() + halfSize, halfSize);
            freeLists.get(availableOrder - 1).add(left);
            freeLists.get(availableOrder - 1).add(right);
            availableOrder--;
        }
 
        MemorySlot allocated = freeLists.get(order).removeFirst();
        syncParentList();
        return new MemorySlot(allocated.getBase(), size);
    }
 
    // ------------------------------------------------------------------
    // Reclaim
    // ------------------------------------------------------------------
 
    @Override
    public void reclaimMemory(ur_os.process.Process p) {
        ur_os.memory.ProcessMemoryManager pmm = p.getPMM();
        switch (pmm.getType()) {
            case SEGMENTATION: {
                ur_os.memory.segmentation.PMM_Segmentation pmms =
                    (ur_os.memory.segmentation.PMM_Segmentation) p.getPMM();
                for (ur_os.memory.segmentation.SegmentTableEntry ste :
                        pmms.getSt().getTable()) {
                    buddyFree(ste.getMemorySlot());
                }
                break;
            }
            default:
            case CONTIGUOUS: {
                ur_os.memory.contiguous.PMM_Contiguous pmmc =
                    (ur_os.memory.contiguous.PMM_Contiguous) p.getPMM();
                buddyFree(pmmc.getMemorySlot());
                pmmc.setValid(false);
                break;
            }
        }
        syncParentList();
    }
 
    // ------------------------------------------------------------------
    // Buddy merge on free
    // ------------------------------------------------------------------
 
    private void buddyFree(MemorySlot slot) {
        int blockSize = nextPowerOfTwo(slot.getSize());
        int order = log2(blockSize);
        int base = slot.getBase();
 
        base = base - (base % blockSize);
 
        while (order < maxOrder) {
            int buddyBase = buddyOf(base, order);
            MemorySlot buddy = findInFreeList(order, buddyBase);
            if (buddy == null) break;
            freeLists.get(order).remove(buddy);
            base = Math.min(base, buddyBase);
            order++;
        }
 
        freeLists.get(order).add(new MemorySlot(base, 1 << order));
    }
 
    private int buddyOf(int base, int order) {
        return base ^ (1 << order);
    }
 
    private MemorySlot findInFreeList(int order, int base) {
        for (MemorySlot s : freeLists.get(order)) {
            if (s.getBase() == base) return s;
        }
        return null;
    }
 
    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
 
    private int orderFor(int size) {
        int order = 0;
        int blockSize = 1;
        while (blockSize < size) {
            blockSize <<= 1;
            order++;
        }
        return order;
    }
 
    private int nextPowerOfTwo(int n) {
        if (n <= 0) return 1;
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }
 
    private int log2(int n) {
        int k = 0;
        while (n > 1) { n >>= 1; k++; }
        return k;
    }
 
    private void syncParentList() {
        list.clear();
        for (int k = 0; k <= maxOrder; k++) {
            for (MemorySlot s : freeLists.get(k)) {
                list.add(new MemorySlot(s));
            }
        }
        list.sort((a, b) -> Integer.compare(a.getBase(), b.getBase()));
    }
 
    // ------------------------------------------------------------------
    // Debug
    // ------------------------------------------------------------------
 
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Buddy System Free Lists ===\n");
        for (int k = maxOrder; k >= 0; k--) {
            if (!freeLists.get(k).isEmpty()) {
                sb.append(String.format("Order %2d (size %6d): ", k, 1 << k));
                for (MemorySlot s : freeLists.get(k)) {
                    sb.append("[base=").append(s.getBase()).append("] ");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
 
