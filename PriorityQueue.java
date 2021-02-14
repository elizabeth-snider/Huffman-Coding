/*  Student information for assignment:
 *
 *  On our honor, Elizabeth and John, this programming assignment is our own work
 *  and we have not provided this code to any other student.
 *
 *  Number of slip days used: 0
 *
 *  Student 1 (Student whose turnin account is being used)
 *  UTEID: eys275
 *  email address: elizabethsnider2011@gmail.com
 *  Grader name: Henry Liu
 *
 *  Student 2
 *  UTEID: jec4968
 *  email address: johnhenrycruz@utexas.edu
 *
 */

import java.util.Iterator;
import java.util.LinkedList;

public class PriorityQueue<E extends Comparable<? super E>> {
    //linked list as the data structure for the priority queue
    LinkedList<E> queue;
    //pointers for the first and last nodes
    TreeNode first;
    TreeNode last;
    //size of the queue
    int size;

    //constructor for the class that creates a new linked list
    // and resets everything
    public PriorityQueue() {
        queue = new LinkedList<E>();
        first = null;
        last = null;
        size = 0;
    }

    //method for enqueueing a node
    //passes the node to be added
    public boolean enqueue(E node) {
        //if the queue is empty, just add the node
        if (last == null) {
            queue.add((E) node);
            first = last = (TreeNode) node;
            size++;
            return true;
            //if the node is the greatest in the queue, add to the end
        } else if (((Integer) last.getFrequency()).compareTo
                ((Integer) ((TreeNode) node).getFrequency()) <= 0) {
            queue.add((E) node);
            last = (TreeNode) node;
            size++;
            return true;
            //if the node is the smallest in the queue, add to the beginning
        } else if (((Integer) first.getFrequency()).compareTo
                ((Integer) ((TreeNode) node).getFrequency()) > 0) {
            queue.addFirst((E) node);
            first = (TreeNode) node;
            size++;
            return true;
            //else find the correct spot to insert the node
        } else {
            Iterator<E> it = iterator();
            int index = 0;
            while (it.hasNext()) {
                index++;
                int val = ((TreeNode) node).getFrequency();
                TreeNode next = (TreeNode) it.next();
                if (((Integer) next.getFrequency()).compareTo
                        ((Integer) ((TreeNode) node).getFrequency()) <= 0) {
                    queue.add(size() - index + 1, node);
                    size++;
                    return true;
                }
            }
        }
        return false;
    }

    //creates a new iterator to traverse the queue
    //starts from the last node of the queue
    public Iterator<E> iterator() {
        return queue.descendingIterator();
    }

    //method for dequeueing the first node of the queue
    public E dequeue() {
        //returns null if the queue is empty
        if (first == null) {
            return null;
        }
        //if the queue only has one node, remove it
        if (size == 1) {
            first = last = null;
            size--;
            return queue.removeFirst();
            //else removes the first node
        } else {
            first = (TreeNode) queue.get(1);
            size--;
            return queue.removeFirst();
        }
    }

    //returns the size of the queue
    public int size() {
        return size;
    }
}
