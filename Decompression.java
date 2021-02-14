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

import java.io.IOException;

public class Decompression extends SimpleHuffProcessor {
    //saved header format
    private static int format;
    //counts frequency array
    private static int[] freq;
    //priority queue for the huffman tree
    private static PriorityQueue<TreeNode> queue;
    //saved root of the tree
    private static TreeNode root;
    //size of the tree
    private static int size;

    //checks if the header of the file is valid
    //returns -1 if not
    //passes the input and out streams
    public static int checkHeader(BitInputStream in, BitOutputStream out)
            throws IOException {
        int inBits = in.readBits(BITS_PER_INT);
        //checks the magic number
        if (inBits != MAGIC_NUMBER) {
            return -1;
        }
        //saves the header format
        format = in.readBits(BITS_PER_INT);
        return 0;
    }

    //recreates the huffman tree
    //passes the inputstream
    public static void recreateTree(BitInputStream in) throws IOException {
        //if the format is counts, create a new queue from the read frequencies
        if (format == STORE_COUNTS) {
            freq = new int[ALPH_SIZE];
            countHeader(in);
            createQueue();
            createTree();
            //if tree format, save the first 32 bits as the size
            //and then create the tree
        } else {
            size = in.readBits(BITS_PER_INT);
            root = treeHeader(in, 0);
        }
    }

    //reads the count header to get the frequencies of
    // each character in the file
    private static void countHeader(BitInputStream in) throws IOException {
        for (int index = 0; index < ALPH_SIZE; index++) {
            int bits = in.readBits(BITS_PER_INT);
            freq[index] = bits;
        }
    }

    //creates a new priority queue for the count format
    //adds nodes with their respective frequencies
    public static void createQueue() {
        queue = new PriorityQueue<TreeNode>();
        for (int index = 0; index < ALPH_SIZE; index++) {
            if (freq[index] != 0) {
                TreeNode node = new TreeNode(index, freq[index]);
                queue.enqueue(node);
            }
        }
        //adds a node for the peof
        queue.enqueue(new TreeNode(PSEUDO_EOF, 1));
    }

    //creates the huffman tree from the priority queue
    public static void createTree() {
        while (queue.size() != 1) {
            TreeNode left = queue.dequeue();
            TreeNode right = queue.dequeue();
            TreeNode parent = new TreeNode(left,
                    left.getFrequency() + right.getFrequency(), right);
            queue.enqueue(parent);
        }
    }

    //reads the tree header bit by bit until the size of the tree is met
    //passes in the inputstream and iterated count
    private static TreeNode treeHeader(BitInputStream in, int count)
            throws IOException {
        int bit = in.readBits(1);
        //adds 1 to the count if the bit is 0
        //or 10 for the node's value and the bit 1
        if (bit == 0) {
            count++;
        } else {
            count += Compression.LEAF_BITS_SIZE;
        }
        TreeNode n = null;
        if (count <= size) {
            //if the bit is 0, uses recursion to set the new node
            //and returns the new node
            if (bit == 0) {
                n = new TreeNode(-1, -1);
                n.setLeft(treeHeader(in, count));
                n.setRight(treeHeader(in, count));
                return n;
                //if the bit is 1, reads the next 9 bits as the
                // new node's value and returns the new node
            } else if (bit == 1) {
                int value = in.readBits(BITS_PER_WORD + 1);
                n = new TreeNode(value, -1);
                return n;
            }
        }
        return n;
    }

    //reads the rest of the compressed file
    //passes the input and output stream
    public static int readData(BitInputStream in, BitOutputStream out)
            throws IOException {
        int bit = in.readBits(1);
        int total = 0;
        //changes the root variable to the first node in
        //the priority queue if the format is counts
        if (format == STORE_COUNTS) {
            root = queue.first;
        }
        TreeNode node = root;
        while (bit != -1) {
            //traverses the tree according to if the bit is 0 (left)
            //or 1 (right)
            if (bit == 0) {
                node = node.getLeft();
            } else {
                node = node.getRight();
            }
            //if you reach a leaf node, write out the node's value
            //and reset the node to the root
            if (node.isLeaf()) {
                //if the node's value is the peof, stop the decompression
                //and return the total number of bits written
                if (node.getValue() == PSEUDO_EOF) {
                    out.close();
                    in.close();
                    return total;
                }
                out.write(node.getValue());
                total += BITS_PER_WORD;
                node = root;
            }
            //read the next bit
            bit = in.readBits(1);
        }
        return total;
    }


}
