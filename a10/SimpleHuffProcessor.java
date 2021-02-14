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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SimpleHuffProcessor implements IHuffProcessor {

	// IHuffViewer object to choose operations
	private IHuffViewer myViewer;
	// store way file will be compressed
	public int format;
	// int to describe num bits that will be written into the file
	public int bitsSaved;
	// int to describe the difference in the size of the original file
	// to the size of the compressed file
	private int bitDiff;

	/**
	 * Compresses input to output, where the same InputStream has previously
	 * been pre-processed via <code>preprocessCompress</code> storing state used
	 * by this call. <br>
	 * pre: <code>preprocessCompress</code> must be called before this method
	 *
	 * @param in    is the stream being compressed (NOT a BitInputStream)
	 * @param out   is bound to a file/stream to which bits are written for the
	 *              compressed file (not a BitOutputStream)
	 * @param force if this is true create the output file even if it is larger
	 *              than the input file. If this is false do not create the
	 *              output file if it is larger than the input file.
	 * @return the number of bits written.
	 * @throws IOException if an error occurs while reading from the input file
	 *                     or writing to the output file.
	 */
	public int compress(InputStream in, OutputStream out, boolean force)
			throws IOException {
		// if force not chosen and compressed file larger than original
		// dont compress
		if (!force && bitDiff < 0) {
			myViewer.showError("compressed file has more bits than uncompressed file. " +
					"select \"force compression\" option to compress.");
			return -1;
		}
		// initialize input and output streams
		BitInputStream bis = new BitInputStream(in);
		BitOutputStream bos = new BitOutputStream(new BufferedOutputStream(out));
		// call method that handles writing compressed content into file
		Compression.write(bis, bos, Compression.headerBits, format);
		// calculate actual size of compressed file
		// accounting for padding by the compiler
		int padding = bitsSaved % BITS_PER_WORD;
		if (padding != 0) {
			bitsSaved += (BITS_PER_WORD - padding);
		}
		myViewer.showMessage("file compressed");
		// return num bits written into file
		return bitsSaved;
	}

	/**
	 * Preprocess data so that compression is possible --- count
	 * characters/create tree/store state so that a subsequent call to compress
	 * will work. The InputStream is <em>not</em> a BitInputStream, so wrap it
	 * int one as needed.
	 *
	 * @param in           is the stream which could be subsequently compressed
	 * @param headerFormat a constant from IHuffProcessor that determines what
	 *                     kind of header to use, standard count format,
	 *                     standard tree format, or possibly some format added
	 *                     in the future.
	 * @return number of bits saved by compression or some other measure Note,
	 * to determine the number of bits saved, the number of bits written
	 * includes ALL bits that will be written including the magic
	 * number, the header format number, the header to reproduce the
	 * tree, AND the actual data.
	 * @throws IOException if an error occurs while reading from the input file.
	 */
	public int preprocessCompress(InputStream in, int headerFormat)
			throws IOException {
		// store what format file should be written into
		format = headerFormat;
		// initialize input stream
		BitInputStream bis = new BitInputStream(in);
		// calculate original size of the file in bits
		int total = Compression.getFreq(bis) * BITS_PER_WORD;
		// create queue
		Compression.createQueue();
		// create huffman tree
		Compression.createTree();
		// create map
		Compression.createMap();
		// find number of bits of magic number
		int magicNumBits = Integer.toBinaryString(MAGIC_NUMBER)
				.length();
		// find number of bits of the header
		int headerFormBits = Integer.toBinaryString(headerFormat).length();
		// find number of bits of the header content
		int headerBits = Compression.headerContent(headerFormat);
		// find number of bits of the size of the tree
		int treeSizeBits = Compression
				.addPadding(headerBits, BITS_PER_INT)
				.length();
		// find number of bits of the actual data
		// based on path through tree
		int dataBits = Compression.actualData();
		// depending on way file is compressed
		// calculate number of bits written into compressed file
		// based on header format
		if (headerFormat == STORE_COUNTS) {
			bitsSaved = (dataBits + magicNumBits + headerFormBits
					+ headerBits);
		} else {
			bitsSaved = (dataBits + magicNumBits + headerFormBits
					+ headerBits + treeSizeBits);
		}
		// calculate difference in bits of original file
		// to new file
		bitDiff = total - bitsSaved;
		myViewer.showMessage("preprocess finished");
		// return difference in file sizes in bits
		return bitDiff;
	}

	public void setViewer(IHuffViewer viewer) {
		myViewer = viewer;
	}

	/**
	 * Uncompress a previously compressed stream in, writing the uncompressed
	 * bits/data to out.
	 *
	 * @param in  is the previously compressed data (not a BitInputStream)
	 * @param out is the uncompressed file/stream
	 * @return the number of bits written to the uncompressed file/stream
	 * @throws IOException if an error occurs while reading from the input file
	 *                     or writing to the output file.
	 */
	public int uncompress(InputStream in, OutputStream out) throws IOException {
		// initialize input and output stream
		BitInputStream bis = new BitInputStream(in);
		BitOutputStream bos = new BitOutputStream(new BufferedOutputStream(out));
		// if not compressed under huffman conditions, dont decompress
		if (Decompression.checkHeader(bis, bos) == -1) {
			myViewer.showError("header is invalid, not a huffman file");
			return -1;
		}
		// recreate huffman tree
		Decompression.recreateTree(bis);
		myViewer.showMessage("file decompressed");
		// write decompressed data into file and return file size
		return Decompression.readData(bis, bos);
	}

	private void showString(String s) {
		if (myViewer != null)
			myViewer.update(s);
	}
}
