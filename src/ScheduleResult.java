public class ScheduleResult {
    private Schedule schedule;
    private boolean isAccepted;
    private double ast;
    private double aft;
    private double responseTime;
    private double makespan;
    private int noOfCloudNodesUsed;
    private int noOfFogNodesUsed;
    private int noOfSlots;
    private double earlyTime;
    private double lateTime;
    private double percentageEarly;
    private double percentageLate;
    private double cloudCost;
    private double averageMakespan;
    private double percentageEdgeOccupancy;
    private double averageResponseTime;
    private TaskDAG taskDAG;

    public ScheduleResult(Schedule schedule) {
        this.schedule = schedule;
        this.taskDAG = schedule.getTaskDAG();
//        this.ast = schedule.getAST();
//        this.aft = schedule.getAFT();
//        this.makespan = schedule.getAFT() - schedule.getAST();
//        this.responseTime = schedule.getAFT() - schedule.getTaskDAG().getArrivalTime();
//        this.makespan = schedule.getAFT() - schedule.getActualStartTimeOfDAG();
        this.noOfSlots = schedule.countSlotsInNetwork();

//        if (this.responseTime <= this.taskDAG.getDeadline()) {
//            this.isAccepted = true;
//            this.earlyTime = this.taskDAG.getDeadline() - this.makespan;
//        } else {
//            this.isAccepted = false;
//            this.lateTime = this.makespan - this.taskDAG.getDeadline();
//        }


    }

    public double getMakespan() {
        return this.makespan;
    }

    public double getResponseTime() {
        return this.responseTime;
    }

    public double getPercentageEdgeOccupancy() {
        this.percentageEdgeOccupancy = 0;
//        int noOfFogs = 0;
        double maxAFT = -1;

        for (int i = 0; i < schedule.getProcessorCoreExecutionSlots().size(); i++) {
            Processor currentProcessor = schedule.getProcessorCoreExecutionSlots().get(i).get(0).getProcessor();

            if (currentProcessor.isFog()) {
//                noOfFogs += 1;
                for (int j = 0; j < schedule.getProcessorCoreExecutionSlots().get(i).size(); j++) {
                    Slot currentSlot = schedule.getProcessorCoreExecutionSlots().get(i).get(j);

                    if (currentSlot.getTask() != null) {
                        this.percentageEdgeOccupancy += currentSlot.getEndTime() - currentSlot.getStartTime();

                        if (currentSlot.getEndTime() > maxAFT) {
                            maxAFT = currentSlot.getEndTime();
                        }
                    }
                }
            }
        }

//        System.out.println("noOfFogs: " + noOfFogs);
//        System.out.println("percentageEdgeOccupancy: " + this.percentageEdgeOccupancy);
        return (double)Math.round(this.percentageEdgeOccupancy * 100 / (this.schedule.getProcessorDAG().getNoOfFogs() * maxAFT) * 100d) / 100d;
    }

    public double getCloudCost() {
        this.cloudCost = 0;

        for (int i = 0; i < schedule.getProcessorCoreExecutionSlots().size(); i++) {
            Processor currentProcessor = schedule.getProcessorCoreExecutionSlots().get(i).get(0).getProcessor();

            if (!currentProcessor.isFog()) {
                for (int j = 0; j < schedule.getProcessorCoreExecutionSlots().get(i).size(); j++) {
                    Slot currentSlot = schedule.getProcessorCoreExecutionSlots().get(i).get(j);

                    if (currentSlot.getTask() != null) {
                        double occupancyPeriod = currentSlot.getEndTime() - currentSlot.getStartTime();

                        this.cloudCost += occupancyPeriod * currentProcessor.getCostPerTimeUnit();
                    }
                }
            }
        }

        return (double)Math.round(this.cloudCost * 100d) / 100d;
    }

    public void print() {
        System.out.println("App id " + this.taskDAG.getId());
        if (isAccepted) {
            System.out.println(PrintUtils.ANSI_GREEN_BACKGROUND + "ACCEPTED!!!" + PrintUtils.ANSI_RESET);
        } else {
            System.out.println(PrintUtils.ANSI_RED_BACKGROUND + "REJECTED!!!" + PrintUtils.ANSI_RESET);
        }

        System.out.println("Response time: " + this.responseTime + ", deadline: " + this.taskDAG.getDeadline());
        System.out.println("Makespan: " + this.makespan + ", Based makespan: " + this.taskDAG.getBasedMakespan());
        System.out.println("Arrives at " + this.taskDAG.getArrivalTime() + ", wait time: " + (this.ast - this.taskDAG.getArrivalTime()));
        System.out.println("AST: " + this.ast + ", AFT: " + this.aft);
        System.out.println("=========================================================");
    }

    public void printWithResourceUsage() {
        System.out.println("App id " + this.taskDAG.getId());
        if (isAccepted) {
            System.out.println(PrintUtils.ANSI_GREEN_BACKGROUND + "ACCEPTED!!!" + PrintUtils.ANSI_RESET);
        } else {
            System.out.println(PrintUtils.ANSI_RED_BACKGROUND + "REJECTED!!!" + PrintUtils.ANSI_RESET);
        }

        System.out.println("Response time: " + this.responseTime + ", deadline: " + this.taskDAG.getDeadline());
        System.out.println("Makespan: " + this.makespan + ", Based makespan: " + this.taskDAG.getBasedMakespan());
        System.out.println("Arrives at " + this.taskDAG.getArrivalTime() + ", wait time: " + (this.ast - this.taskDAG.getArrivalTime()));
        System.out.println("AST: " + this.ast + ", AFT: " + this.aft);

        this.noOfFogNodesUsed = schedule.getNoOfTasksAllocatedToFogNodes();
        this.noOfCloudNodesUsed = schedule.getNoOfTasksAllocatedToCloudNodes();

        this.cloudCost = 0;

        for (int i = 0; i < schedule.getProcessorCoreExecutionSlots().size(); i++) {
            Processor currentProcessor = schedule.getProcessorCoreExecutionSlots().get(i).get(0).getProcessor();

            if (!currentProcessor.isFog()) {
                for (int j = 0; j < schedule.getProcessorCoreExecutionSlots().get(i).size(); j++) {
                    Slot currentSlot = schedule.getProcessorCoreExecutionSlots().get(i).get(j);

                    if (currentSlot.getTask() != null) {
                        double occupancyPeriod = currentSlot.getEndTime() - currentSlot.getStartTime();

                        this.cloudCost += occupancyPeriod * currentProcessor.getCostPerTimeUnit();
                    }
                }
            }
        }

        System.out.println("CCR: " + this.taskDAG.getCCR() +
                ", noOfClouds: " + this.noOfCloudNodesUsed + ", noOfFogs: " + this.noOfFogNodesUsed);
        System.out.println("Cloud cost: " + this.cloudCost);
//        if (this.isAccepted) {
//            System.out.println("No of slots: " + this.noOfSlots);
//        }
        System.out.println("=========================================================");
    }

    public boolean isAccepted() {
        return this.isAccepted;
    }
}
