import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;

public class MainTest {
    public static void main(String[] args) {
        LinkedList<Slot> queue = new LinkedList<Slot>();
        queue.add(new Slot(null, null, 1, 2));
        queue.add(new Slot(null, null, 2, 3));
        queue.add(new Slot(null, null, 3, 4));

        ListIterator<Slot> iterator = queue.listIterator(queue.size());
        while (iterator.hasPrevious()) {
            int currentSlotIndex = iterator.previousIndex();
            System.out.println(currentSlotIndex);
            Slot currentSlot = iterator.previous();

            if (currentSlot.getTask() == null &&
                    (queue.size() > 1 && queue.get(currentSlotIndex - 1).getTask() == null)) {
                currentSlot.setStartTime(iterator.previous().getStartTime());
                iterator.remove();
                iterator.next();
            }
        }

        for (int i = 0; i < queue.size(); i++) {
            System.out.println("Item " + i + ": start=" + queue.get(i).getStartTime() + ", end=" + queue.get(i).getEndTime());
        }
    }
}
