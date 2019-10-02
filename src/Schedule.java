import java.util.ArrayList;
import java.util.Collections;

public class Schedule {
    private TaskDAG taskDAG;
    private ProcessorDAG processorDAG;
//    private ArrayList<Slot> taskExecutionSlot;
    private ArrayList<ArrayList<Slot>> processorCoreExecutionSlots;
    private double aft;
    private double ast;

    public Schedule(ProcessorDAG processorDAG) {
        this.taskDAG = null;
        this.processorDAG = processorDAG;

//        count variable for indexing processorCores
        int schedulePositionId = 0;
        this.processorCoreExecutionSlots = new ArrayList<ArrayList<Slot>>();
        for (int i = 0; i < processorDAG.getProcessors().size(); i++) {
            Processor currentProcessor = processorDAG.getProcessors().get(i);

            for (int j = 0; j < currentProcessor.getNoOfCores(); j++) {
                ProcessorCore newCore = new ProcessorCore(currentProcessor, j, schedulePositionId);
                Slot newSlot = new Slot(null, newCore, 0, Integer.MAX_VALUE);

                ArrayList<Slot> newListOfSlots = new ArrayList<Slot>();
                newListOfSlots.add(newSlot);

                this.processorCoreExecutionSlots.add(newListOfSlots);

                schedulePositionId += 1;
            }
        }

//        this.taskExecutionSlot = new ArrayList<Slot>();
//        for (int i = 0; i < this.taskDAG.getTasks().size(); i++) {
//            this.taskExecutionSlot.add(null);
//        }
    }

    public void removeSlot(int processorCoreIndex, int slotIndex) {

    }

    //    add a new slot for a task on a processor at time startTime
    public void addNewSlot(ProcessorCore processorCore, Task task, double startTime) {
        ArrayList<Slot> currentProcessorCoreSlots = this.processorCoreExecutionSlots.get(
                processorCore.getSchedulePositionId()
        );

        double computationTime = task.getComputationRequired() / processorCore.getProcessor().getProcessingRate();

        int count = 0;
        for (int i = 0; i < currentProcessorCoreSlots.size(); i++) {
            Slot currentSlot = currentProcessorCoreSlots.get(i);
            count = i;
            double actualStartTime = Math.max(startTime, currentSlot.getStartTime());
            double endTime = actualStartTime + computationTime;

//            find the first slot on the processor that fits the startTime and endTime
            if (currentSlot.getTask() == null &&
                    currentSlot.getStartTime() <= actualStartTime &&
                    currentSlot.getEndTime() >= endTime) {
                Slot newSlot = new Slot(task, processorCore, startTime, endTime);
                currentProcessorCoreSlots.add(newSlot);

                Slot slotBefore = new Slot(null, processorCore, currentSlot.getStartTime(), startTime);
                Slot slotAfter = new Slot(null, processorCore, endTime, currentSlot.getEndTime());

                if (startTime != currentSlot.getStartTime() && endTime != currentSlot.getEndTime()) {
                    currentProcessorCoreSlots.add(slotBefore);
                    currentProcessorCoreSlots.add(slotAfter);
                } else if (startTime == currentSlot.getStartTime() && endTime != currentSlot.getEndTime()) {
                    currentProcessorCoreSlots.add(slotAfter);
                } else if (startTime != currentSlot.getStartTime() && endTime == currentSlot.getEndTime()) {
                    currentProcessorCoreSlots.add(slotBefore);
                }

                currentProcessorCoreSlots.remove(i);

                Collections.sort(currentProcessorCoreSlots, Slot.compareByStartTime);

//                this.taskExecutionSlot.set(task.getId(), newSlot);
                task.setAllocatedSlot(newSlot);
                break;
            }
        }

        if (count == currentProcessorCoreSlots.size() - 1) {
            System.out.println("Found no suitable slots on processor " + processorCore.getProcessor().getId() +
                    " core " + processorCore.getCoreId() + "!");
        }
    }

//    this function calculates the earliest slot that a processor
//    which will be able to execute a specified task
    public Slot getFirstFitSlotForTaskOnProcessorCore(ProcessorCore processorCore, Task task) {
        double deadline = task.getTaskDAG().getDeadline() + task.getTaskDAG().getArrivalTime();
//        the ready time of the task at which
//        all required input data has arrived at the current processor
        double readyTime = -1;

//        loop through all predecessors of the current task
//        to calculate readyTime
        for (int i = 0; i < task.getPredecessors().size(); i++) {
//            get predecessor task in the tuple of (task, dependency)
            Task predTask = task.getPredecessors().get(i).getTask();
//            get dependency of current predecessor
            double predTaskConstraint = task.getPredecessors().get(i).getDataConstraint();
//            get processor which processes the current predecessor task
//            System.out.println("current pred: " + predTask.getId());
//            ProcessorCore predProcessorCore = this.taskExecutionSlot.get(predTask.getId()).getProcessorCore();
            ProcessorCore predProcessorCore;

            if (predTask.getAllocatedSlot() == null) {
                return null;
            }

            predProcessorCore = predTask.getAllocatedSlot().getProcessorCore();

//            calculate communication time to transmit data dependency from
//            processor which is assigned to process the predecessor task to
//            the processor which is being considered to use to process the current task
            double communicationTime = this.processorDAG.getCommunicationTimeBetweenCores(
                    predProcessorCore, processorCore, predTaskConstraint
            );

//            double predSlotEndTime = this.taskExecutionSlot.get(predTask.getId()).getEndTime();
            double predSlotEndTime = predTask.getAllocatedSlot().getEndTime();
            double currentReadyTime = predSlotEndTime + communicationTime;

            if (currentReadyTime > readyTime) {
                readyTime = currentReadyTime;
            }
        }

        if (readyTime > deadline) {
            return null;
        }

        double processingTime = task.getComputationRequired() / processorCore.getProcessor().getProcessingRate();
        ArrayList<Slot> currentProcessorSlots = this.processorCoreExecutionSlots.get(
                processorCore.getSchedulePositionId()
        );

//        find the earliest slot - from end to start
        Slot earliestSlot = null;
        for (int i = currentProcessorSlots.size() - 1; i >= 0; i--) {
            Slot currentSlot = currentProcessorSlots.get(i);

            if (currentSlot.getEndTime() < readyTime) {
                break;
            } else {
                if (currentSlot.getTask() == null) {
                    double actualStart = Math.max(currentSlot.getStartTime(), readyTime);
                    double actualEnd = actualStart + processingTime;

                    if (actualEnd <= currentSlot.getEndTime()) {
                        earliestSlot = new Slot(task, processorCore, actualStart, actualEnd);
                    }
                }
            }
        }

        if (earliestSlot.getStartTime() > deadline || earliestSlot.getEndTime() > deadline) {
            return null;
        }

//        if (earliestSlot != null) {
            return earliestSlot;
//        } else {
//            System.out.println("Nothing");
//            return new Slot(task, processorCore, Double.MAX_VALUE, Double.MAX_VALUE);
//        }
    }

    public double getComputationCostOfTask(Task task) {
        return this.getComputationCostOfTask(task.getId());
    }

    public double getComputationCostOfTask(int taskId) {
        Slot taskSlot = this.taskDAG.getTasks().get(taskId).getAllocatedSlot();

        return taskSlot.getTask().getComputationRequired() / taskSlot.getProcessor().getProcessingRate();
    }

    public int getNoOfTasksAllocatedToCloudNodes() {
        int noOfTasksAllocatedToCloudNodes = 0;

        for (int i = 1; i < this.taskDAG.getTasks().size() - 1; i++) {
            if (!this.taskDAG.getTasks().get(i).getAllocatedSlot().getProcessor().isFog()) {
                noOfTasksAllocatedToCloudNodes += 1;
            }
        }

        return noOfTasksAllocatedToCloudNodes;
    }

    public int getNoOfTasksAllocatedToFogNodes() {
        return this.taskDAG.getTasks().size() - this.getNoOfTasksAllocatedToCloudNodes() - 2;
    }

    public ProcessorCore getFirstProcessorCoreFreeAt(double time) {
        ProcessorCore selectedProcessorCore = null;
        double earliestStartTime = Double.MAX_VALUE;

        for (int positionId = 0; positionId < this.processorCoreExecutionSlots.size(); positionId++) {
            for (int slotId = 0; slotId < this.processorCoreExecutionSlots.get(positionId).size(); slotId++) {
                Slot currentSlot = this.processorCoreExecutionSlots.get(positionId).get(slotId);

                if (currentSlot.getTask() == null && currentSlot.getEndTime() >= time) {
                    if (Math.max(currentSlot.getStartTime(), time) <= earliestStartTime) {
                        earliestStartTime = Math.max(currentSlot.getStartTime(), time);
                        selectedProcessorCore = this.processorCoreExecutionSlots.get(positionId).get(0).getProcessorCore();
                    }
                }
            }
        }

        return selectedProcessorCore;
    }

    public int countSlotsInNetwork() {
        int count = 0;

        for (int i = 0; i < this.processorCoreExecutionSlots.size(); i++) {
            count += this.processorCoreExecutionSlots.get(i).size();
        }

        return count;
    }

    public int countOccupiedSlotsInNetwork() {
        int count = 0;

        for (int i = 0; i < this.processorCoreExecutionSlots.size(); i++) {
            for (int j = 0; j < this.processorCoreExecutionSlots.get(i).size(); j++) {
                if (this.processorCoreExecutionSlots.get(i).get(j).getTask() != null) {
                    count += 1;
                }
            }
        }

        return count;
    }

    public void setAFT(double aft) {
        this.aft = aft;
    }

    public double getAFT() {
        return this.aft;
    }

    public void setAST(double ast) {
        this.ast = ast;
    }

    public double getAST() {
        return this.ast;
    }

    public ProcessorDAG getProcessorDAG() {
        return this.processorDAG;
    }

    public ArrayList<ArrayList<Slot>> getProcessorCoreExecutionSlots() {
        return this.processorCoreExecutionSlots;
    }

    public void setProcessorExecutionSlots(ArrayList<ArrayList<Slot>> processorCoreExecutionSlots) {
        this.processorCoreExecutionSlots = processorCoreExecutionSlots;
    }

//    public ArrayList<Slot> getTaskExecutionSlot() {
//        return this.taskExecutionSlot;
//    }

    public void showProcessorSlots() {
        for (int i = 0; i < this.processorCoreExecutionSlots.size(); i++) {
            ArrayList<Slot> currentProcessorCoreSlots = this.processorCoreExecutionSlots.get(i);

            if (currentProcessorCoreSlots.size() > 1) {
                System.out.print("Processor " + currentProcessorCoreSlots.get(0).getProcessor().getId() +
                        ", core " + currentProcessorCoreSlots.get(0).getProcessorCore().getCoreId() + ": ");
                for (int j = 0; j < currentProcessorCoreSlots.size(); j++) {
                    Slot currentSlot = currentProcessorCoreSlots.get(j);
                    System.out.print("[");
                    if (currentSlot.getTask() == null) {
                        System.out.print(PrintUtils.ANSI_RED_BACKGROUND + "n" + PrintUtils.ANSI_RESET);
                    } else {
                        System.out.print(PrintUtils.ANSI_GREEN_BACKGROUND + currentSlot.getTask().getId() + PrintUtils.ANSI_RESET);
                    }
                    System.out.print(currentSlot.getStartTime() + "-" + currentSlot.getEndTime() + "]--");
                }
                System.out.println();
            }
        }
    }

    public TaskDAG getTaskDAG() {
        return this.taskDAG;
    }

    public double getActualStartTimeOfDAG() {
//        return this.taskExecutionSlot.get(1).getStartTime();
        double actualStartTime = Double.MAX_VALUE;

        for (int i = 0; i < this.taskDAG.getLayers().get(1).size(); i++) {
            Task currentTask = this.taskDAG.getLayers().get(1).get(i);
            Slot currentTaskSlot = this.taskDAG.getTasks().get(currentTask.getId()).getAllocatedSlot();

            if (actualStartTime > currentTaskSlot.getStartTime()) {
                actualStartTime = currentTaskSlot.getStartTime();
            }
        }

        return actualStartTime;
    }
}
