/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.virtualmemory;

import java.util.LinkedList;
import java.util.Random;
import ur_os.memory.paging.PageTable;
import ur_os.memory.paging.PageTableEntry;

/**
 * Random page replacement algorithm.
 * Selects a random valid page as the victim.
 *
 * @author user
 */
public class PVMM_Random extends ProcessVirtualMemoryManager {

    Random r;

    public PVMM_Random() {
        type = ProcessVirtualMemoryManagerType.RANDOM;
        r = new Random();
    }

    @Override
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
}
