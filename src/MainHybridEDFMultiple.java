import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

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
        
        String pathToProcessors = "dataset-GHz/new-processors.dag";
        ProcessorDAG processorDAG = new ProcessorDAG(pathToProcessors);

        Schedule schedule = new Schedule(processorDAG);

        double currentTime = 0;

        // initialize a list of DAGs
        for (int dagIndex = 1; dagIndex <= noOfDAGsToTest; dagIndex++) {
            TaskDAG taskDAG = new TaskDAG(dagIndex, GlobalConfig.DATASET_PATH + dagIndex + ".dag");
            currentTime = taskDAG.getArrivalTime();

            // calculate priority of new DAG's tasks
            LinkedList<Task> newTaskPriority = MainHybridEDFMultiple.prioritizeTasks(taskDAG, processorDAG);

            // after having calculated all the tasks
            for (int i = 0; i < newTaskPriority.size(); i++) {
                queueOfTasks.add(newTaskPriority.get(i));
            }

            // loop through all slots on each processor core and temporarily remove all the tasks that are
            // scheduled to be executed after current time
            for (int procesorCoreIndex = 0; procesorCoreIndex < processorDAG.getTotalNumberOfCores(); procesorCoreIndex++) {
                ArrayList<Slot> currentProcessorSlots = schedule.getProcessorCoreExecutionSlots().get(procesorCoreIndex);
                int noOfSlots = currentProcessorSlots.size();

                for (int slotIndex = 0; slotIndex < noOfSlots; slotIndex++) {
                    Slot currentSlot = currentProcessorSlots.get(slotIndex);

                    if (currentSlot.getStartTime() > currentTime) {
                        Task removedTask = currentSlot.getTask();
                        removedTask.setAllocatedSlot(null);
                        queueOfTasks.add(removedTask);

                        schedule.removeSlot(procesorCoreIndex, slotIndex);
                    }
                }
            }

            // sort all the tasks that still remain in the queue based on job's deadline and task priority
            Task.sortByDeadlinePriority(queueOfTasks);

            Iterator<Task> iterator = queueOfTasks.iterator();
            while (iterator.hasNext()) {
                // get rid of all the tasks whose job's deadline are before current time
                if (iterator.next().getTaskDAG().getDeadline() < currentTime) {
                    iterator.remove();
                }
            }

//            for (int taskIndex = 0; taskIndex < queueOfTasks.size())

            while (queueOfTasks.size() > 0) {
                Task currentTask = 
            }

            Task entryTask = queueOfTasks.removeLast();
        }
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
