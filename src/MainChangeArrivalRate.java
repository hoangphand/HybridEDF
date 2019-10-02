public class MainChangeArrivalRate {
    public static void main(String[] args) {
        int noOfDAGs = GlobalConfig.DATASET_SIZE;
        double arrivalRate = 0.5;
        double arrivalTime = arrivalRate;
        String dirDataSet = GlobalConfig.DATASET_PATH;

        for (int index = 2; index < noOfDAGs + 1; index++) {
            System.out.println(index);
            TaskDAG taskDAG = new TaskDAG(1, dirDataSet + "/" + index + ".dag");
            taskDAG.setArrivalTime(arrivalTime);
            taskDAG.exportDAG(dirDataSet + "/" + index + ".dag");

            arrivalTime =  arrivalTime + arrivalRate;
        }
    }
}
