/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ur_os.memory.freememorymagament;

/**
 * @author super
 */
public class BestFitMemorySlotManager extends FreeMemorySlotManager {

  public BestFitMemorySlotManager(int memSize) {
    super(memSize);
  }

  @Override
  public MemorySlot getSlot(int size) {
    MemorySlot m = null;

    MemorySlot bestSlot = null;
    int bestAmount = 100000;

    for (MemorySlot memorySlot : list) {
      if (memorySlot.getSize() <= bestAmount && memorySlot.canContain(size)) {
        bestSlot = memorySlot;
        bestAmount = memorySlot.getBase();
      }
    }

    if (bestAmount == size) {
      m = bestSlot;
      list.remove(m);
      return m;
    } else {

      m = bestSlot.assignMemory(size);
      return m;
    }

    return m;
  }
}
