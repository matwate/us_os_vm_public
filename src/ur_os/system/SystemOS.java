/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ur_os.system;

import java.util.ArrayList;
import java.util.Random;
import ur_os.memory.Memory;
import ur_os.memory.MemoryInstruction;
import ur_os.memory.MemoryManagerType;
import ur_os.memory.MemoryOperationType;
import ur_os.memory.freememorymagament.FreeFramesManager;
import ur_os.memory.freememorymagament.FreeMemorySlotManager;
import ur_os.memory.paging.PMM_Paging;
import ur_os.process.EndInstruction;
import ur_os.process.IOInstruction;
import ur_os.process.Instruction;
import ur_os.process.Process;
import ur_os.virtualmemory.SwapMemory;

/**
 * @author super
 */
public class SystemOS implements Runnable {

  SimulationType simType;
  private static int clock = 0;
  private static final int MAX_SIM_CYCLES = 1000;
  private static final int MAX_SIM_PROC_CREATION_TIME = 50;
  private static final double PROB_PROC_CREATION = 0.1;
  public static final int MAX_PROC_SIZE = 1000;
  private static Random r = new Random(1235);
  private OS os;
  private CPU cpu;
  private IOQueue ioq;

  private Memory memory;
  private SwapMemory swap;

  public static final int SEED_SEGMENTS = 7401;
  public static final int SEED_PROCESS_SIZE = 9630;

  public static final int MEMORY_SIZE = 12 * 1024 + 1;
  public static final int SWAP_MEMORY_SIZE = 1_073_741_824; // 1 GB

  protected ArrayList<Process> processes;
  ArrayList<Integer> execution;

  private double totalExternalFragmentation = 0;
  private double totalMemoryUtilization = 0;
  private int totalHoles = 0;
  private double peakExternalFragmentation = 0;
  private int peakHoles = 0;
  private double totalInternalFragmentation = 0;
  private double peakInternalFragmentation = 0;
  private int fragmentationSamples = 0;

  public SystemOS(SimulationType simType) {
    memory = new Memory(MEMORY_SIZE);
    swap = new SwapMemory(MEMORY_SIZE);
    cpu = new CPU(memory, swap);
    ioq = new IOQueue();
    os = new OS(this, cpu, ioq);
    cpu.setOS(os);
    ioq.setOS(os);
    execution = new ArrayList();
    processes = new ArrayList();
    // initSimulationQueue();
    // initSimulationQueueSimple();
    initSimulationQueueSimpler4();

    showProcesses();
    this.simType = simType;
  }

  public int getTime() {
    return clock;
  }

  public ArrayList<Process> getProcessAtI(int i) {
    ArrayList<Process> ps = new ArrayList();

    for (Process process : processes) {
      if (process.getTime_init() == i) {
        ps.add(process);
      }
    }

    return ps;
  }

  public void initSimulationQueue() {
    double tp;
    Process p;
    for (int i = 0; i < MAX_SIM_PROC_CREATION_TIME; i++) {
      tp = r.nextDouble();
      if (PROB_PROC_CREATION >= tp) {
        p = new Process();
        p.setTime_init(clock);
        processes.add(p);
      }
      clock++;
    }
    clock = 0;
  }

  public void initSimulationQueueSimple() {
    Process p;
    int cont = 0;
    for (int i = 0; i < MAX_SIM_PROC_CREATION_TIME; i++) {
      if (i % 4 == 0) {
        p = new Process(cont++, -1, true);
        p.setTime_init(clock);
        processes.add(p);
      }
      clock++;
    }
    clock = 0;
  }

  public void initSimulationQueueSimpler() {

    int tempSize;
    Process p = new Process(0, 0);
    tempSize = r.nextInt(MAX_PROC_SIZE - 1) + 1;
    p.setSize(tempSize);
    Instruction temp;
    p.addCPUInstructions(3);
    temp =
        new MemoryInstruction(
            MemoryOperationType.LOAD,
            r.nextInt(tempSize),
            (byte) -1,
            4); // Load from logical address 5, 4 clock cycles
    p.addInstruction(temp);
    p.addCPUInstructions(3);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process 1
    p = new Process(1, 2);
    tempSize = r.nextInt(MAX_PROC_SIZE - 1) + 1;
    p.setSize(tempSize);
    p.addCPUInstructions(3);
    // temp = new IOInstruction(5);
    temp =
        new MemoryInstruction(
            MemoryOperationType.STORE,
            r.nextInt(tempSize),
            (byte) 38,
            3); // Store in logical address 10, valir 38, 3 clock cycles
    p.addInstruction(temp);
    p.addCPUInstructions(3);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process 2
    p = new Process(2, 6);
    tempSize = r.nextInt(MAX_PROC_SIZE - 1) + 1;
    p.setSize(tempSize);
    p.addCPUInstructions(7);
    // temp = new IOInstruction(3);
    temp =
        new MemoryInstruction(
            MemoryOperationType.LOAD,
            r.nextInt(tempSize),
            (byte) -1,
            4); // Load from logical address 62, 4 clock cycles
    p.addInstruction(temp);
    p.addCPUInstructions(5);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process 3
    p = new Process(3, 8);
    tempSize = r.nextInt(MAX_PROC_SIZE - 1) + 1;
    p.setSize(tempSize);
    p.addCPUInstructions(4);
    // temp = new IOInstruction(3);
    temp =
        new MemoryInstruction(
            MemoryOperationType.STORE,
            r.nextInt(tempSize),
            (byte) 42,
            4); // Store in logical address 10, valir 38, 3 clock cycles
    p.addInstruction(temp);
    p.addCPUInstructions(7);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    clock = 0;
  }

  public void initSimulationQueueSimpler3() {

    Process p = new Process(0, 0);
    p.setSize(200);
    Instruction temp;
    p.addCPUInstructions(5);
    temp = new IOInstruction(4);
    p.addInstruction(temp);
    p.addCPUInstructions(3);
    processes.add(p);

    // Process 1
    p = new Process(1, 5);
    p.setSize(500);
    p.addCPUInstructions(13);
    temp = new IOInstruction(5);
    p.addInstruction(temp);
    p.addCPUInstructions(16);
    processes.add(p);

    // Process 2
    p = new Process(2, 6);
    p.setSize(250);
    p.addCPUInstructions(7);
    temp = new IOInstruction(3);
    p.addInstruction(temp);
    p.addCPUInstructions(5);
    processes.add(p);

    // Process 3
    p = new Process(3, 24);
    p.setSize(800);
    p.addCPUInstructions(4);
    temp = new IOInstruction(3);
    p.addInstruction(temp);
    p.addCPUInstructions(7);
    processes.add(p);

    // Process 4
    p = new Process(4, 31);
    p.setSize(600);
    p.addCPUInstructions(7);
    temp = new IOInstruction(3);
    p.addInstruction(temp);
    p.addCPUInstructions(7);
    processes.add(p);

    clock = 0;
  }

  public void initSimulationQueueSimpler2() {

    Process p = new Process(false);
    Instruction temp;
    p.addCPUInstructions(15);
    temp = new IOInstruction(12);
    p.addInstruction(temp);
    p.addCPUInstructions(21);
    p.setTime_init(0);
    p.setPid(0);
    processes.add(p);

    p = new Process(false);
    p.addCPUInstructions(8);
    temp = new IOInstruction(4);
    p.addInstruction(temp);
    p.addCPUInstructions(16);
    p.setTime_init(2);
    p.setPid(1);
    processes.add(p);

    p = new Process(false);
    p.addCPUInstructions(10);
    temp = new IOInstruction(15);
    p.addInstruction(temp);
    p.addCPUInstructions(12);
    p.setTime_init(6);
    p.setPid(2);
    processes.add(p);

    p = new Process(false);
    p.addCPUInstructions(9);
    temp = new IOInstruction(6);
    p.addInstruction(temp);
    p.addCPUInstructions(17);
    p.setTime_init(8);
    p.setPid(3);
    processes.add(p);

    clock = 0;
  }

  public void initSimulationQueueSimpler4() {

    // Process P0 - arrival: 0, size: 2048, 3 CPU → LOAD(4) → 3 CPU → END
    Process p = new Process(0, 0);
    p.setSize(2048);
    Instruction temp;
    p.addCPUInstructions(3);
    temp = new MemoryInstruction(MemoryOperationType.LOAD, 0, (byte) -1, 4);
    p.addInstruction(temp);
    p.addCPUInstructions(3);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process P1 - arrival: 1, size: 1024, 2 CPU → STORE(3) → 2 CPU → END
    p = new Process(1, 1);
    p.setSize(1024);
    p.addCPUInstructions(2);
    temp = new MemoryInstruction(MemoryOperationType.STORE, 0, (byte) 0, 3);
    p.addInstruction(temp);
    p.addCPUInstructions(2);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process P2 - arrival: 2, size: 3072, 6 CPU → LOAD(4) → 6 CPU → END
    p = new Process(2, 2);
    p.setSize(3072);
    p.addCPUInstructions(6);
    temp = new MemoryInstruction(MemoryOperationType.LOAD, 0, (byte) -1, 4);
    p.addInstruction(temp);
    p.addCPUInstructions(6);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process P3 - arrival: 15, size: 1536, 4 CPU → STORE(4) → 4 CPU → END
    p = new Process(3, 15);
    p.setSize(1536);
    p.addCPUInstructions(4);
    temp = new MemoryInstruction(MemoryOperationType.STORE, 0, (byte) 0, 4);
    p.addInstruction(temp);
    p.addCPUInstructions(4);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process P4 - arrival: 22, size: 2048, 3 CPU → LOAD(3) → 3 CPU → END
    p = new Process(4, 22);
    p.setSize(2048);
    p.addCPUInstructions(3);
    temp = new MemoryInstruction(MemoryOperationType.LOAD, 0, (byte) -1, 3);
    p.addInstruction(temp);
    p.addCPUInstructions(3);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    // Process P5 - arrival: 30, size: 768, 2 CPU → STORE(3) → 2 CPU → END
    p = new Process(5, 30);
    p.setSize(768);
    p.addCPUInstructions(2);
    temp = new MemoryInstruction(MemoryOperationType.STORE, 0, (byte) 0, 3);
    p.addInstruction(temp);
    p.addCPUInstructions(2);
    temp = new EndInstruction();
    p.addInstruction(temp);
    processes.add(p);

    clock = 0;
  }

  public boolean isSimulationFinished() {

    boolean finished = true;

    for (Process p : processes) {
      finished = finished && p.isFinished();
    }

    return finished;
  }

  public SimulationType getSimulationType() {
    return simType;
  }

  public int getClock() {
    return clock;
  }

  @Override
  public void run() {
    double tp;
    ArrayList<Process> ps;

    System.out.println("******SIMULATION START******");

    int i = 0;
    Process temp_exec;
    int tempID;
    while (!isSimulationFinished()
        && i < MAX_SIM_CYCLES) { // MAX_SIM_CYCLES is the maximum simulation time, to avoid infinite
      // loops
      System.out.println("******Clock: " + i + "******");

      if (i == 8) {
        i = i;
      }

      if (this.getSimulationType() == SimulationType.ALL
          || this.getSimulationType() == SimulationType.PROCESS_PLANNING) {
        System.out.println(cpu);
        System.out.println(ioq);
      }

      // Crear procesos, si aplica en el ciclo actual
      ps = getProcessAtI(i);
      for (Process p : ps) {
        os.create_process(p);
        System.out.println("Process Created: " + p.getPid() + "\n" + p);

        showFreeMemory();
      } // If the scheduler is preemtive, this action will trigger the extraction from the CPU, is
      // any process is there.

      // Actualizar el OS, quien va actualizar el Scheduler

      os.update();
      // os.update() prepares the system for execution. It runs at the beginning of the cycle.

      clock++;

      temp_exec = cpu.getProcess();
      if (temp_exec == null) {
        tempID = -1;
      } else {
        tempID = temp_exec.getPid();
      }
      execution.add(tempID);

      // Actualizar la CPU
      cpu.update();

      /// Actualizar la IO
      ioq.update();

      // Las actualizaciones de CPU y IO pueden generar interrupciones que actualizan a cola de
      // listos, cuando salen los procesos

      if (this.getSimulationType() == SimulationType.ALL
          || this.getSimulationType() == SimulationType.PROCESS_PLANNING) {
        System.out.println("After the cycle: ");
        System.out.println(cpu);
        System.out.println(ioq);
      }

      updateFragmentationMetrics();

      i++;
    }
    System.out.println("******SIMULATION FINISHES******");
    // os.showProcesses();

    System.out.println("******Process Execution******");
    for (Integer num : execution) {
      System.out.print(num + " ");
    }
    System.out.println("");

    System.out.println("******Performance Indicators******");
    System.out.println("Total execution cycles: " + clock);
    System.out.printf("CPU Utilization: %.2f%%%n", this.calcCPUUtilization() * 100);
    System.out.printf("Throughput: %.4f processes/cycle%n", this.calcThroughput());
    System.out.printf("Average Turnaround Time: %.2f cycles%n", this.calcTurnaroundTime());
    System.out.printf("Average Waiting Time: %.2f cycles%n", this.calcAvgWaitingTime());
    System.out.printf(
        "Average Context Switches: %.2f switches/process%n", this.calcAvgContextSwitches());
    System.out.printf("Average Response Time: %.2f cycles%n", this.calcAvgResponseTime());
    if (fragmentationSamples > 0) {
      double avgEF = totalExternalFragmentation / fragmentationSamples;
      double avgMU = totalMemoryUtilization / fragmentationSamples;
      double avgHoles = (double) totalHoles / fragmentationSamples;
      if (OS.SMM == MemoryManagerType.PAGING) {
        double avgIF = totalInternalFragmentation / fragmentationSamples;
        System.out.printf("Avg Internal Fragmentation: %.2f%%%n", avgIF * 100);
        System.out.printf("Peak Internal Fragmentation: %.2f%%%n", peakInternalFragmentation * 100);
        System.out.printf("Avg Memory Utilization: %.2f%%%n", avgMU * 100);
      } else {
        System.out.printf("Avg External Fragmentation: %.2f%%%n", avgEF * 100);
        System.out.printf("Peak External Fragmentation: %.2f%%%n", peakExternalFragmentation * 100);
        System.out.printf("Avg Number of Holes: %.2f holes%n", avgHoles);
        System.out.printf("Peak Number of Holes: %d holes%n", peakHoles);
        System.out.printf("Avg Memory Utilization: %.2f%%%n", avgMU * 100);
      }
      System.out.println(
          "Total Memory: " + MEMORY_SIZE + " bytes (" + (MEMORY_SIZE / 1024) + " KB)");
    }

    // showProcesses();
    // memory.showNotNullBytes();

    // showFreeMemory();
  }

  public void showFreeMemory() {
    if (OS.SMM == MemoryManagerType.PAGING) {
      System.out.println("Free frame number: " + os.fmm.getSize());
    } else {
      System.out.println("Free Memory Slots (" + os.fmm.getSize() + "): ");
      FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
      System.out.println(msm);
    }
  }

  public void showProcesses() {
    System.out.println("Process list:");
    StringBuilder sb = new StringBuilder();

    for (Process process : processes) {
      sb.append(process);
      sb.append("\n");
    }

    System.out.println(sb.toString());
  }

  public double calcCPUUtilization() {
    int cont = 0;
    for (Integer num : execution) {
      if (num == -1) cont++;
    }

    return (execution.size() - cont) / (double) execution.size();
  }

  public double calcExternalFragmentation() {
    if (OS.SMM == MemoryManagerType.PAGING) return 0.0;
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return msm.getExternalFragmentation();
  }

  public double calcInternalFragmentation() {
    if (OS.SMM != MemoryManagerType.PAGING) return 0.0;
    int totalAllocated = 0;
    int totalWasted = 0;
    for (Process p : processes) {
      if (p.getPMM() != null && p.getPMM().getType() == MemoryManagerType.PAGING) {
        PMM_Paging pmm = (PMM_Paging) p.getPMM();
        int pages = pmm.getPT().getSize();
        int allocated = pages * OS.PAGE_SIZE;
        int wasted = allocated - pmm.getSize();
        totalAllocated += allocated;
        totalWasted += wasted;
      }
    }
    if (totalAllocated == 0) return 0.0;
    return (double) totalWasted / totalAllocated;
  }

  public double calcMemoryUtilization() {
    if (OS.SMM == MemoryManagerType.PAGING) {
      FreeFramesManager ffm = (FreeFramesManager) os.fmm;
      int totalFrames = SystemOS.MEMORY_SIZE / OS.PAGE_SIZE;
      return ffm.getMemoryUtilization(totalFrames);
    }
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return 1.0 - msm.getMemoryUtilization(MEMORY_SIZE);
  }

  public int calcTotalFreeMemory() {
    if (OS.SMM == MemoryManagerType.PAGING) return 0;
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return msm.getTotalFreeMemory();
  }

  public int calcLargestFreeSlot() {
    if (OS.SMM == MemoryManagerType.PAGING) return 0;
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return msm.getLargestFreeSlot();
  }

  public int calcSmallestFreeSlot() {
    if (OS.SMM == MemoryManagerType.PAGING) return 0;
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return msm.getSmallestFreeSlot();
  }

  public int calcFreeSlotCount() {
    if (OS.SMM == MemoryManagerType.PAGING) return 0;
    FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
    return msm.getSize();
  }

  public double calcTurnaroundTime() {

    double tot = 0;

    for (Process p : processes) {
      tot = tot + (p.getTime_finished() - p.getTime_init());
    }

    return tot / processes.size();
  }

  public double calcThroughput() {
    return (double) processes.size() / execution.size();
  }

  public double calcAvgWaitingTime() {
    double tot = 0;

    for (Process p : processes) {
      tot = tot + ((p.getTime_finished() - p.getTime_init()) - p.getTotalExecutionTime());
    }

    return tot / processes.size();
  }

  public double calcAvgContextSwitches() {
    int cont = 1;
    int prev = execution.get(0);
    for (Integer i : execution) {
      if (prev != i) {
        cont++;
        prev = i;
      }
    }

    return cont / (double) processes.size();
  }

  public double calcAvgResponseTime() {

    double tot = 0;
    int temp = 0;
    for (Process p : processes) {
      temp = execution.indexOf(p.getPid()); // On which cycle did the process started execution
      tot = tot + (temp - p.getTime_init()); // Difference between execution start and arrival
    }

    return tot / processes.size();
  }

  private void updateFragmentationMetrics() {
    fragmentationSamples++;
    if (OS.SMM == MemoryManagerType.PAGING) {
      // Paging: track internal fragmentation and memory utilization
      FreeFramesManager ffm = (FreeFramesManager) os.fmm;
      int totalFrames = SystemOS.MEMORY_SIZE / OS.PAGE_SIZE;
      double mu = ffm.getMemoryUtilization(totalFrames);
      double ifrag = calcInternalFragmentation();

      totalMemoryUtilization += mu;
      totalInternalFragmentation += ifrag;

      if (ifrag > peakInternalFragmentation) peakInternalFragmentation = ifrag;
    } else {
      // Contiguous / Segmentation: track external fragmentation, holes, utilization
      FreeMemorySlotManager msm = (FreeMemorySlotManager) os.fmm;
      double ef = msm.getExternalFragmentation();
      double mu = 1.0 - msm.getMemoryUtilization(MEMORY_SIZE);
      int holes = msm.getSize();

      totalExternalFragmentation += ef;
      totalMemoryUtilization += mu;
      totalHoles += holes;

      if (ef > peakExternalFragmentation) peakExternalFragmentation = ef;
      if (holes > peakHoles) peakHoles = holes;
    }
  }
}
