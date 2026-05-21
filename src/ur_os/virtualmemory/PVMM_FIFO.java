/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.virtualmemory;

import java.util.LinkedList;
import ur_os.memory.paging.PageTable;
import ur_os.memory.paging.PageTableEntry;

/**
 *
 * @author user
 */
public class PVMM_FIFO extends ProcessVirtualMemoryManager{

    public PVMM_FIFO(){
        type = ProcessVirtualMemoryManagerType.FIFO;
    }
    
    @Override
    public int getVictim(LinkedList<Integer> memoryAccesses, PageTable pt) {
        // Find the valid page with the oldest (smallest) clock value
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
    
}
