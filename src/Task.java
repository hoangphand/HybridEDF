import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class Task {
    // x 10^9 CPU cycles
    private static final double TASK_COMPUTATION_REQUIRED_LOWER_BOUND = 2f;
    private static final double TASK_COMPUTATION_REQUIRED_UPPER_BOUND = 60f;
    // MB: megabyte
    private static final double TASK_MEMORY_REQUIRED_LOWER_BOUND = 0.5f;
    private static final double TASK_MEMORY_REQUIRED_UPPER_BOUND = 20f;
    // MB: megabyte
    private static final double TASK_STORAGE_REQUIRED_LOWER_BOUND = 0.5f;
    private static final double TASK_STORAGE_REQUIRED_UPPER_BOUND = 20f;
    // Mb: megabit
    private static final double TASK_MEMORY_CONSTRAINT_LOWER_BOUND = 0.2f;
    private static final double TASK_MEMORY_CONSTRAINT_UPPER_BOUND = 5f;

    private int id;
    private int taskDAGId;
    private TaskDAG taskDAG;
    private int layerId;
    private double computationRequired;
    private double memoryRequired;
    private double storageRequired;
    private Slot allocatedSlot;

    private double priority;

    private ArrayList<DataDependency> predecessors;
    private ArrayList<DataDependency> successors;

    public Task(int id, int taskDAGId, int layerId, double computationRequired, double memoryRequired, double storageRequired) {
        this.id = id;
        this.taskDAGId = taskDAGId;

        this.computationRequired = computationRequired;
        this.memoryRequired = memoryRequired;
        this.storageRequired = storageRequired;

        this.predecessors = new ArrayList<DataDependency>();
        this.successors = new ArrayList<DataDependency>();
    }

    public Task(int id, int taskDAGId) {
        this.id = id;
        this.taskDAGId = taskDAGId;

        this.predecessors = new ArrayList<DataDependency>();
        this.successors = new ArrayList<DataDependency>();
    }

    public Task(int id, TaskDAG taskDAG) {
        this.id = id;
        this.taskDAG = taskDAG;

        this.predecessors = new ArrayList<DataDependency>();
        this.successors = new ArrayList<DataDependency>();
    }

    public Task() {
        this.predecessors = new ArrayList<DataDependency>();
        this.successors = new ArrayList<DataDependency>();
    }

    public double getComputationRequired() {
        return this.computationRequired;
    }

    public void setComputationRequired(double computationRequired) {
        this.computationRequired = computationRequired;
    }

    public double getMemoryRequired() {
        return this.memoryRequired;
    }

    public void setMemoryRequired(double memoryRequired) {
        this.memoryRequired = memoryRequired;
    }

    public double getStorageRequired() {
        return this.storageRequired;
    }

    public void setStorageRequired(double storageRequired) {
        this.storageRequired = storageRequired;
    }

    private void addPredecessor(Task task, double dataConstraint) {
        DataDependency dataDependency = new DataDependency(task, dataConstraint);

        this.predecessors.add(dataDependency);
    }

    private void addSuccessor(Task task, double dataConstraint) {
        DataDependency dataDependency = new DataDependency(task, dataConstraint);

        this.successors.add(dataDependency);
    }

    public void addEdge(Task task, double dataConstraint) {
        this.addSuccessor(task, dataConstraint);
        task.addPredecessor(this, dataConstraint);
    }

    private void removePredecessor(Task task) {
        for (int i = 0; i <= this.predecessors.size(); i++) {
            if (this.predecessors.get(i).getTask().id == task.id) {
                this.predecessors.remove(i);

                break;
            }
        }
    }

    private void removeSuccessor(Task task) {
        for (int i = 0; i < this.successors.size(); i++) {
            if (this.successors.get(i).getTask().id == task.id) {
                this.successors.remove(i);

                break;
            }
        }
    }

    public void removeEdge(Task task) {
        this.removePredecessor(task);
        this.removeSuccessor(task);
        task.removePredecessor(this);
        task.removeSuccessor(this);
    }

    public int getLayerId() {
        return this.layerId;
    }

    public void setLayerId(int layerId) {
        this.layerId = layerId;
    }

    public void generateRandomValues() {
        this.memoryRequired = RandomUtils.getRandomDoubleInRange(TASK_MEMORY_REQUIRED_LOWER_BOUND, TASK_MEMORY_REQUIRED_UPPER_BOUND);
        this.computationRequired = RandomUtils.getRandomDoubleInRange(TASK_COMPUTATION_REQUIRED_LOWER_BOUND, TASK_COMPUTATION_REQUIRED_UPPER_BOUND);
        this.storageRequired = RandomUtils.getRandomDoubleInRange(TASK_STORAGE_REQUIRED_LOWER_BOUND, TASK_STORAGE_REQUIRED_UPPER_BOUND);
    }

    public void addEdgeRandomConstraint(Task task, double ccr, ProcessorDAG processorDAG) {
        double avgBandwidth = (processorDAG.getAvgDownloadBandwidth() + processorDAG.getAvgUploadBandwidth()) / 2;
        double avgProcessingRate = processorDAG.getAvgProcessingRate();

        double correspondingCommunication = ccr * this.computationRequired * avgBandwidth / avgProcessingRate;
        double lowerBound = 0.9f * correspondingCommunication;
        double upperBound = 1.1f * correspondingCommunication;

        double randomMemoryConstraint = RandomUtils.getRandomDoubleInRange(lowerBound, upperBound);

        this.addSuccessor(task, randomMemoryConstraint);
        task.addPredecessor(task, randomMemoryConstraint);
    }

    public ArrayList<DataDependency> getPredecessors() {
        return this.predecessors;
    }

    public ArrayList<DataDependency> getSuccessors() {
        return this.successors;
    }

    public int getId() {
        return this.id;
    }

    public double getPriority() {
        return this.priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public void setTaskDAGId(int taskDAGId) {
        this.taskDAGId = taskDAGId;
    }

    public int getTaskDAGId() {
        return this.taskDAG.getId();
    }

    public TaskDAG getTaskDAG() {
        return this.taskDAG;
    }

    public void setTaskDAG(TaskDAG taskDAG) {
        this.taskDAG = taskDAG;
    }

    public void setAllocatedSlot(Slot slot) {
        this.allocatedSlot = slot;
    }

    public Slot getAllocatedSlot() {
        return this.allocatedSlot;
    }

    public static Comparator<Task> compareByPriority = new Comparator<Task>() {
        @Override
        public int compare(Task task1, Task task2) {
            return Double.compare(task1.getPriority(), task2.getPriority());
        }
    };

    public static void sortByPriority(LinkedList<Task> listOfTasks) {
        Collections.sort(listOfTasks, Task.compareByPriority);
    }

    public static Comparator<Task> compareByDeadline = new Comparator<Task>() {
        @Override
        public int compare(Task task1, Task task2) {
            return Double.compare(task1.taskDAG.getDeadline() + task1.taskDAG.getArrivalTime(),
                    task2.taskDAG.getDeadline() + task2.taskDAG.getArrivalTime());
        }
    };

    public static void sortByDeadline(LinkedList<Task> listOfTasks) {
        Collections.sort(listOfTasks, Task.compareByDeadline);
    }

    public static Comparator<Task> compareByDeadlinePriority = new Comparator<Task>() {
        @Override
        public int compare(Task task1, Task task2) {
            double deadline1 = task1.taskDAG.getDeadline() + task1.taskDAG.getArrivalTime();
            double deadline2 = task2.taskDAG.getDeadline() + task2.taskDAG.getArrivalTime();

            if (deadline1 != deadline2) {
                return Double.compare(deadline2, deadline1);
//                return Double.compare(deadline1, deadline2);
            }

            return Double.compare(task1.getPriority(), task2.getPriority());
        }
    };

    public static void sortByDeadlinePriority(LinkedList<Task> listOfTasks) {
        Collections.sort(listOfTasks, Task.compareByDeadlinePriority);
    }
}
