import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

public class MainHybridEDFMultiple extends JFrame {

    private static final long serialVersionUID = 1L;

    public MainHybridEDFMultiple(String title, String chartLabel, Schedule schedule) {
        super(title);
        SchedulingGanttChart ganttChart = new SchedulingGanttChart(this, chartLabel, schedule);
    }

    public static void main(String[] args) {
        int noOfDAGsToTest = 10;
//        int noOfDAGsToTest = GlobalConfig.DATASET_SIZE;
        LinkedList<TaskDAG> listOfDags = new LinkedList<TaskDAG>();
        LinkedList<Task> queueOfTasks = new LinkedList<Task>();
        
        String pathToProcessors = "dataset/new-processors.dag";
        ProcessorDAG processorDAG = new ProcessorDAG(pathToProcessors);

        Schedule schedule = new Schedule(processorDAG);

        double currentTime = 0;
        int countNoRemovedTasks = 0;

        // initialize a list of DAGs
        for (int dagIndex = 1; dagIndex <= noOfDAGsToTest; dagIndex++) {
            TaskDAG taskDAG = new TaskDAG(dagIndex, GlobalConfig.DATASET_PATH + dagIndex + ".dag");
            currentTime = taskDAG.getArrivalTime();
//            System.out.println("current time: " + currentTime);

            listOfDags.add(taskDAG);

            // calculate priority of new DAG's tasks
            LinkedList<Task> newTaskPriority = MainHybridEDFMultiple.prioritizeTasks(taskDAG, processorDAG);

            // put them all in the queue after having calculated their priority
            for (int i = 0; i < newTaskPriority.size(); i++) {
                queueOfTasks.add(newTaskPriority.get(i));
            }

            // loop through all slots on each processor core and temporarily remove all the tasks that are
            // already scheduled to be executed after current time from the schedule and move them to the queue
            for (int procesorCoreIndex = 0; procesorCoreIndex < processorDAG.getTotalNumberOfCores(); procesorCoreIndex++) {
                ArrayList<Slot> currentProcessorSlots = schedule.getProcessorCoreExecutionSlots().get(procesorCoreIndex);
                int noOfSlots = currentProcessorSlots.size();

                // remove tasks and move them to queue
                for (int slotIndex = noOfSlots - 1; slotIndex >= 0; slotIndex--) {
                    Slot currentSlot = currentProcessorSlots.get(slotIndex);

                    if (currentSlot.getTask() != null && currentSlot.getStartTime() < currentTime) {
                        break;
                    } else if (currentSlot.getTask() != null && currentSlot.getStartTime() > currentTime) {
                        Task removedTask = currentSlot.getTask();
                        removedTask.setAllocatedSlot(null);
                        queueOfTasks.add(removedTask);

                        currentSlot.setTask(null);
                    }
                }

                // clean up redundant slots to free resources
                ListIterator<Slot> iterator = currentProcessorSlots.listIterator(currentProcessorSlots.size());
                while (iterator.hasPrevious()) {
                    int currentSlotIndex = iterator.previousIndex();
                    Slot currentSlot = iterator.previous();

                    if (currentSlot.getTask() != null && currentSlot.getStartTime() < currentTime) {
                        break;
                    } else if (currentSlot.getTask() == null &&
                            (currentProcessorSlots.size() > 1 &&
                                    currentProcessorSlots.get(currentSlotIndex - 1).getTask() == null)) {
                        // move the iterator ahead one step to combine with
                        currentSlot.setStartTime(iterator.previous().getStartTime());
                        // current slot is the one ahead
                        // remove current slot
                        iterator.remove();
                        // step backward in the list one time
                        iterator.next();
                    }
                }
            }

            // sort all the tasks that still remain in the queue based on job's deadline and task priority
            Task.sortByDeadlinePriority(queueOfTasks);

            Iterator<Task> iterator = queueOfTasks.iterator();
            while (iterator.hasNext()) {
                // get rid of all the tasks whose job's deadline are before current time
                TaskDAG currentTaskDag = iterator.next().getTaskDAG();
                if (currentTaskDag.getDeadline() + currentTaskDag.getArrivalTime() < currentTime) {
                    iterator.remove();
                    countNoRemovedTasks++;
                }
            }

            // scheduling
//            boolean isNewRound = true;
            while (queueOfTasks.size() > 0) {
                Task currentTask = queueOfTasks.removeLast();

                if (currentTask.getId() == 0) {
                    TaskDAG currentDag = currentTask.getTaskDAG();
                    ProcessorCore selectedProcessorCore = schedule.getFirstProcessorCoreFreeAt(currentDag.getArrivalTime());

                    schedule.addNewSlot(selectedProcessorCore, currentTask, currentDag.getArrivalTime());
                } else {
                    Slot selectedSlot = null;
                    ProcessorCore selectedProcessorCore = null;

                    for (int i = 0; i < schedule.getProcessorCoreExecutionSlots().size(); i++) {
                        ProcessorCore currentProcessorCore = schedule.getProcessorCoreExecutionSlots().get(i).get(0).getProcessorCore();

                        if (currentTask.getTaskDAG().getCCR() > 1) {
                            // communication intensive
                            if (!currentProcessorCore.getProcessor().isFog()) {
                                continue;
                            }
                        } else {
                            // computation intensive
                            if (currentProcessorCore.getProcessor().isFog()) {
                                continue;
                            }
                        }
                        // find the first fit slot on the current processor core for the current task
                        Slot currentSelectedSlot = schedule.getFirstFitSlotForTaskOnProcessorCore(currentProcessorCore, currentTask);

                        if (selectedSlot == null || currentSelectedSlot.getEndTime() < selectedSlot.getEndTime()) {
                            selectedSlot = currentSelectedSlot;
                            selectedProcessorCore = currentProcessorCore;
                        }
                    }

                    schedule.addNewSlot(selectedProcessorCore, currentTask, selectedSlot.getStartTime());
                }
            }
        }

        System.out.println(countNoRemovedTasks);

        int noOfAcceptedDags = 0;
        int totalNoOfTasks = 0;
        for (int i = 0; i < noOfDAGsToTest; i++) {
            TaskDAG currentTaskDag = listOfDags.get(i);
            totalNoOfTasks += currentTaskDag.getTasks().size();

            if (currentTaskDag.getTasks().get(currentTaskDag.getTasks().size() - 1).getAllocatedSlot() == null) {
                int noOfTasksExecuted = 0;

                for (int j = 0; j < currentTaskDag.getTasks().size(); j++) {
                    if (currentTaskDag.getTasks().get(j).getAllocatedSlot() != null) {
                        noOfTasksExecuted++;
                    }
                }

                System.out.println(String.valueOf(i + 1) + ": Rejected. Arrived at: " + currentTaskDag.getArrivalTime() +
                        ". No of tasks executed: " + noOfTasksExecuted +
                        "/" + currentTaskDag.getTasks().size());
            } else {
                Slot slotForExitTask = currentTaskDag.getTasks().get(currentTaskDag.getTasks().size() - 1).getAllocatedSlot();
                double makespan = slotForExitTask.getEndTime() - currentTaskDag.getArrivalTime();
                System.out.println(String.valueOf(i + 1) + ": Accepted. Arrived at: " +
                        currentTaskDag.getArrivalTime() + " Makespan: " + makespan +
                        ", deadline: " + currentTaskDag.getDeadline());
            }
        }

        System.out.println("No of occupied slots: " + schedule.countOccupiedSlotsInNetwork());
        System.out.println("Total no of tasks: " + totalNoOfTasks);

        SwingUtilities.invokeLater(() -> {
            MainHybridEDFMultiple example = new MainHybridEDFMultiple(
                    "MainHybridEDFMultiple",
                    "MainHybridEDFMultiple",
                    schedule);
            example.setSize(800, 400);
            example.setLocationRelativeTo(null);
            example.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            example.setVisible(true);
        });
    }

    public static LinkedList<Task> prioritizeTasks(TaskDAG taskDAG, ProcessorDAG processorDAG) {
        double avgProcessingRate = processorDAG.getAvgProcessingRate();
        double avgBandwidth = processorDAG.getAvgBandwidthWithLAN();
//        double avgBandwidth = processorDAG.getAvgBandwidth();

        LinkedList<Task> listOfTasks = new LinkedList<Task>();

        for (int i = 0; i < taskDAG.getTasks().size(); i++) {
            listOfTasks.add(taskDAG.getTasks().get(i));
        }

        for (int i = 0; i < taskDAG.getLayers().size(); i++) {
            int layerIndex = taskDAG.getLayers().size() - i - 1;

            for (int j = 0; j < taskDAG.getLayers().get(layerIndex).size(); j++) {
                int taskIndex = taskDAG.getLayers().get(layerIndex).size() - j - 1;
                Task currentTask = taskDAG.getLayers().get(layerIndex).get(taskIndex);

                double avgComputationCost = currentTask.getComputationRequired() / avgProcessingRate;

                if (currentTask.getSuccessors().size() == 0) {
                    currentTask.setPriority(avgComputationCost);
                } else {
                    double tmpMaxSuccessorCost = Double.MIN_VALUE;

                    for (int k = 0; k < currentTask.getSuccessors().size(); k++) {
                        DataDependency currentSuccessor = currentTask.getSuccessors().get(k);
                        Task successorTask = currentSuccessor.getTask();

                        double communicationCost = currentSuccessor.getDataConstraint() / avgBandwidth;
                        double currentSuccessorCost = communicationCost + successorTask.getPriority();

                        if (currentSuccessorCost > tmpMaxSuccessorCost) {
                            tmpMaxSuccessorCost = currentSuccessorCost;
                        }
                    }

                    currentTask.setPriority(avgComputationCost + tmpMaxSuccessorCost);
                }
            }
        }

        listOfTasks.get(0).setPriority(listOfTasks.get(0).getPriority() + 1);

        Task.sortByPriority(listOfTasks);

        return listOfTasks;
    }
}
