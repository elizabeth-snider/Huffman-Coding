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
import java.util.HashMap;

public class Compression extends SimpleHuffProcessor {
    // int to describe the number of bits that would
    // contribute to the size of the tree if
    // it was a leaf node
    public static final int LEAF_BITS_SIZE = 10;
    // int to describe the radix for converting
    // binary strings to int
    public static final int BITS_RADIX = 2;
    // array to store the frequency of ascii chars
    private static int[] freq;
    // priority queue used to create huffman tree
    private static PriorityQueue<TreeNode> queue;
    // map of the values and their respective
    // paths through the huffman tree
    private static HashMap<Integer, String> map;
    // string to represent the contents of the
    // header
    public static String headerBits;

    // method to get the frequency of every value seen in
    // the original file
    public static int getFreq(BitInputStream bis) throws IOException {
        int total = 0;
        freq = new int[ALPH_SIZE];
        // read in 8 bits at a time
        int inBits = bis.readBits(BITS_PER_WORD);
        // loop through file until no more values
        while (inBits != -1) {
            // update total number of values seen in the original file
            total++;
            // update frequency of seen value
            freq[inBits]++;
            // move to next set of 8 bits
            inBits = bis.readBits(BITS_PER_WORD);
        }
        bis.close();
        // return total instances of values in the
        // original file
        return total;
    }

    // method to create the priority queue
    // sorting the values with the least frequent
    // value at the front
    public static void createQueue() {
        queue = new PriorityQueue<TreeNode>();
        // loop through freq array
        for (int index = 0; index < ALPH_SIZE; index++) {
            // if seen in file, add to queue
            if (freq[index] != 0) {
                // store value and its frequency to a treenode
                TreeNode node = new TreeNode(index, freq[index]);
                queue.enqueue(node);
            }
        }
        // queue PEOF
        queue.enqueue(new TreeNode(PSEUDO_EOF, 1));
    }

    // method to create the huffman tree
    public static void createTree() {
        // repeat until only 1 element (root of tree) in the queue
        while (queue.size() != 1) {
            // dequeue first two elements
            TreeNode left = queue.dequeue();
            TreeNode right = queue.dequeue();
            // create parent node with the freq being
            // the combine freq of two nodes that were dequeued
            // make the two nodes the children of the new node
            // in respective order
            TreeNode parent = new TreeNode(left,
                    left.getFrequency() + right.getFrequency(), right);
            // add to queue to be sorted based on its frequency
            queue.enqueue(parent);
        }
    }

    // method to handle the recursive method that finds the
    // paths to all the leaf nodes aka nodes with values
    // add path to map with key as the value
    public static void createMap() {
        // store path in string
        String path = "";
        map = new HashMap<Integer, String>();
        // recursive call
        traverseTree(queue.first, path);
    }

    // recursive method to find all the paths to leaf nodes
    // in the huffman tree
    private static void traverseTree(TreeNode node, String path) {
        // base case
        // once reaching node, add path created to map
        // with respective value
        if (node.isLeaf()) {
            map.put(node.getValue(), path);
            // if not leaf node
            // keep transversing tree
            // update path
        } else {
            // preorder transversal
            traverseTree(node.getLeft(), path + "0");
            traverseTree(node.getRight(), path + "1");
        }
    }

    // method to calculate the number of bits
    // to be written based on the actual data
    public static int actualData() {
        // int to store num bits of actual data
        int result = 0;
        // loop through freq array
        for (int index = 0; index < freq.length; index++) {
            // if a value is seen
            if (freq[index] > 0) {
                // take path length aka num of bits to get to value
                // times its freq
                result += map.get(index).length() * freq[index];
            }
        }
        // add path length to PEOF
        result += map.get(PSEUDO_EOF).length();
        // return total bits of actual data
        return result;
    }

    // method that returns the length of the header based on format
    public static int headerContent(int headerFormat) {
        // if storing counts, return size of alpha
        // times 32 bits for the size of an int
        if (headerFormat == STORE_COUNTS) {
            return ALPH_SIZE * BITS_PER_INT;
            // if tree, transverse preorder
        } else {
            String content = "";
            TreeNode root = queue.first;
            // recursion
            headerBits = readTree(root, content);
            // return length of tree data
            return headerBits.length();
        }
    }

    // recursive method that finds tree header as a string
    public static String readTree(TreeNode node, String content) {
        // when reaching a leaf node
        // store 1 for being a leaf node and the value of the
        // node in 9 bits
        if (node.isLeaf()) {
            String binary = Integer.toBinaryString(node.getValue());
            // if binary string not 9 bits long
            // make 9 bits long
            if (binary.length() < BITS_PER_WORD + 1) {
                String add = "";
                for (int i = 0; i < (BITS_PER_WORD + 1)
                        - binary.length(); i++) {
                    add += "0";
                }
                binary = add.concat(binary);
            }
            return content + "1" + binary;
            // if not leaf node
            // update path, keep doing preorder transversal
        } else {
            return readTree(node.getLeft(), content + "0")
                    + readTree(node.getRight(), "");
        }
    }

    // method to handle all the writing of the content into the output
    public static void write(BitInputStream in, BitOutputStream out,
                             String treeBits, int format) throws IOException {
        // write magic numebr in 32 bits
        out.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        // depending on format, write in header format in 32 bits
        if (format == STORE_COUNTS) {
            // write counts header
            out.writeBits(BITS_PER_INT, STORE_COUNTS);
            // write header content for counts
            writeCountHeader(out);
        } else {
            // write tree header
            out.writeBits(BITS_PER_INT, STORE_TREE);
            // write out tree size in 32 bits
            out.writeBits(BITS_PER_INT, treeBits.length());
            // write header content for tree
            writeTreeHeader(queue.first, out);
        }
        // write out actual data
        // paths to get to each value
        int inBits = in.readBits(BITS_PER_WORD);
        while (inBits != -1) {
            out.writeBits(map.get(inBits).length(), Integer.parseInt(map.get(inBits), BITS_RADIX));
            inBits = in.readBits(BITS_PER_WORD);
        }
        // write in PEOF
        out.writeBits(map.get(PSEUDO_EOF).length(),
                Integer.parseInt(map.get(PSEUDO_EOF), BITS_RADIX));
        out.close();
        in.close();
    }

    // method to write the header if the format is to store counts
    private static void writeCountHeader(BitOutputStream out) {
        // loop through all elements of the freq array
        // store each index's value in 32 bits
        for (int index = 0; index < ALPH_SIZE; index++) {
            out.writeBits(BITS_PER_INT, freq[index]);
        }
    }

    // method to write the header i the format is to store the tree
    // recurse through tree in preorder fashion
    private static void writeTreeHeader(TreeNode node, BitOutputStream out) throws IOException {
        // when reaching a node in the huffman tree
        if (node.isLeaf()) {
            // get value in 9 bit binary form
            String padded = addPadding(node.getValue(), BITS_PER_WORD + 1);
            // write out the value with 1 in front to notate
            // we are interacting with a leaf node
            out.writeBits(LEAF_BITS_SIZE, Integer.parseInt(("1" + padded), BITS_RADIX));
        } else {
            // if node is non value node, store a 1 bit 0 to notate this
            // continue traveling tree preorderly
            out.writeBits(1, 0);
            writeTreeHeader(node.getLeft(), out);
            writeTreeHeader(node.getRight(), out);
        }
    }

    // method to write the value of the nodes in 9 bits
    public static String addPadding(int numBits, int size) {
        // convert value to binary string
        String binary = Integer.toBinaryString(numBits);
        int length = binary.length();
        String result = "";
        // add appropriate 0's in front to make string
        // 9 bits long
        if (length < size) {
            for (int index = 0; index < (size - length); index++) {
                result += "0";
            }
        }
        // combine front padding of 0's to original binary string
        result = result + binary;
        // return 9 bit version of value as a string
        return result;
    }

}
