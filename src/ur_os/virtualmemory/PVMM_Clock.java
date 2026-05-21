/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.virtualmemory;

import java.util.LinkedList;
import ur_os.memory.paging.PageTable;
import ur_os.memory.paging.PageTableEntry;

/**
 * Clock (Second Chance) page replacement algorithm.
 * Scans for the first valid page with referenced=false.
 * If all pages are referenced, clears all reference bits and returns the first valid page.
 *
 * @author user
 */
public class PVMM_Clock extends ProcessVirtualMemoryManager {

    public PVMM_Clock() {
        type = ProcessVirtualMemoryManagerType.CLOCK;
    }

    @Override
    public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
        // Collect indices of all valid pages
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

        // Second pass: clear all reference bits and return first valid page
        for (int idx : validIndices) {
            pt.getList().get(idx).setReferenced(false);
        }
        return validIndices.get(0);
    }
}
