/*
 * Matan Danino: 304802887
 * Shir Elbaz: 204405690
 * Gal Arus: 204372619
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
public class Compressor {
	//Implementation of the LZSS compression algorithm
	final int BACK_WINDOW_SIZE;
	final int MAXIMUN_MATCH_SIZE ;
	final int MINIMUN_MATCH_SIZE;
	Compressor(int back, int front, int match){
		BACK_WINDOW_SIZE = back;
		MAXIMUN_MATCH_SIZE = front;
		MINIMUN_MATCH_SIZE = match;
	}
	public void LZSS_compress(String[] input, String[] output) throws IOException {
		//This method call to the private method CompressLZSS with lzssOnly flags
		CompressLZSS(input, output, false, false);
	}
	public void LZSS_decompress(String[] input, String[] output) throws IOException {
		//This method call to the private method DecompressLZSS
		DecompressLZSS(input, output);
	}
	public void compressMoveToFront(String[] input, String[] output) throws IOException {
		//This Compression method activate a move to front algorithm on the uncoded file
		// and after that perform a LZSS compression
		String[] tmpFile = {input[0] + "tmp"};
		MoveToFrontCompress(input, tmpFile);
		CompressLZSS(tmpFile, output, true, false);//flags set to moveToFront mode
		new File(tmpFile[0]).delete();//delete temp file
	}
	public void decompressMoveToFront(String[] input, String[] output) throws IOException {
		//This decompression method perform a LZSS decompression
		//and after that decode the a move to front algorithm to represent the original file byte's
		String[] tmpFile = {input[0] + "tmp"};
		DecompressLZSS(input,tmpFile);
		MoveToFrontDecompress(tmpFile, output);
		new File(tmpFile[0]).delete();//delete temp file
	}
	public void compressDelta(String[] input, String[] output) throws IOException {
		//This Compression methods activate a Delta Code algorithm on the uncompressed file
		// and after that perform a LZSS compression
		String[] tmpFile = {input[0] + "tmp"};
		DeltaCodeCompress(input, tmpFile);
		CompressLZSS(tmpFile, output, false , true);//flags set to deltaCode mode
		new File(tmpFile[0]).delete();//delete temp file
	}
	public void decompressDelta(String[] input, String[] output) throws IOException {
		//This decompression method perform a LZSS decompression
		//and after that decode the a Delta Code algorithm to represent the original file byte's
		String[] tmpFile = {input[0] + "tmp"};
		DecompressLZSS(input, tmpFile);
		DeltaCodeDecompress(tmpFile, output);
		new File(tmpFile[0]).delete();//delete temp file
	}
	private void CompressLZSS(String[] input_names, String[] output_names, Boolean moveToFront, Boolean delta) throws IOException {
		//Getting the file to encode and path to save the encoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream in = new BufferedInputStream(inFile);
		BufferedOutputStream outBuffer = new BufferedOutputStream(outFile);
		BitBuffer out = new BitBuffer(outBuffer, null);
		if(!moveToFront && !delta)//write two bytes that represent the compress method
			out.write(0, 16);//only LZSS
		else {
			out.write(1, 8);
			if(moveToFront)
				out.write(0,8);//move to front
			else
				out.write(1, 8);//delta code
		}
		out.write(BACK_WINDOW_SIZE, 8);//write the back window size and max match(in bits)
		out.write(MAXIMUN_MATCH_SIZE, 8);
		int backWindowBytes = (int)Math.pow(2, BACK_WINDOW_SIZE);//calculate the size of window and max match
		int maxMatch = (int)Math.pow(2, MAXIMUN_MATCH_SIZE);
		StringBuffer backBuffer = new StringBuffer(backWindowBytes);//create string buffer for the back window
		int nextByte;//variable for the main loop
		String bestMatch = "";
		int index = -1;
		int tmpIndex = -1;	
		
		while((nextByte = in.read()) != -1) {
			if(nextByte < 0 )//read the next byte and check if positive
				nextByte+=256;//update if not positive
			tmpIndex = backBuffer.indexOf(bestMatch + (char)nextByte);//add char to best match and search in the window
			if(tmpIndex != -1 && bestMatch.length() + 1 < maxMatch ) {//if match found and the lookahead buffer have more characters
				bestMatch+=(char)nextByte;//update best match and index
				index = tmpIndex;
			}//go to check back window length
			else {//if match not found(end of main loop)
				if(bestMatch.length() >= MINIMUN_MATCH_SIZE) {//check if the best match is longer then the minimum size
					out.write("1");//write the first bit the represent a token 
					out.write(index, BACK_WINDOW_SIZE);//write the string index and length, the number of bits 
					out.write(bestMatch.length(), MAXIMUN_MATCH_SIZE);//depending the window and max match size
					tmpIndex = -1;//update variable and back window
					index = -1;
					backBuffer.append(bestMatch);
					bestMatch = "" + (char)(nextByte);
				}//go to check window length(end of main loop)
				else{//if best match size is smaller then the minimum match size(write the original byte)
					bestMatch+=(char)(nextByte);//update best match
					while((tmpIndex = backBuffer.indexOf(bestMatch)) == -1 && bestMatch != "") {//while match not found and best match not empty
						out.write("0");//write the first bit the represent a byte
						out.write((bestMatch.charAt(0)), 8);//write the byte value
						backBuffer.append(bestMatch.charAt(0));//update back window and best match
						bestMatch = bestMatch.substring(1);
					}					
				}//go to check window length(end of main loop)
			}
			if (backBuffer.length() > backWindowBytes)//check window size and delete the beginning of the window if need 
	            backBuffer = backBuffer.delete(0, backBuffer.length() - backWindowBytes);
		}
		while(bestMatch.length() > 0 && bestMatch != "") {//if after read all bytes best match not empty
			if(index != -1 && bestMatch.length() > MINIMUN_MATCH_SIZE) {//if best match contain legal match
				out.write("1");//write match token
				out.write(index, BACK_WINDOW_SIZE);
				out.write(bestMatch.length(), MAXIMUN_MATCH_SIZE);
				bestMatch = "";
			}
			else {//write byte token
				out.write("0");
				out.write((byte)(bestMatch.charAt(0)), 8);
				bestMatch = bestMatch.substring(1);
			}
		}
		in.close();//close buffers
		out.close();
	}
	private void DecompressLZSS(String[] input_names, String[] output_names) throws IOException {		
		//Getting the file to decode and address to save the decoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream inBuffer = new BufferedInputStream(inFile);
		BufferedOutputStream out = new BufferedOutputStream(outFile);
		BitBuffer in = new BitBuffer(null, inBuffer);
		//ignore the first two byte's that describe the file compression method
		inBuffer.read();inBuffer.read();
		//Read the window and max match size(in bits) from the beginning of the file 
		final int BACK_WINDOW = inBuffer.read();
		final int MAX_MATCH = inBuffer.read();
		int backWindowByts = (int)Math.pow(2, BACK_WINDOW);//calculate the size in bytes
		StringBuffer backBuffer = new StringBuffer(backWindowByts);
		int nextIndex;//create variables for the main loop
		int nextLen;
		int nextBit;
		//perform encoding according to the LZSS algorithm
		//byte token -> <mode = 0, char(8 bits)> 
		//match token -> <mode = 1, position(BACK_WINDOW bits), length(MAX_MATCH bits)>
		while((nextBit = in.read(1)) != -1) {//read the mode bit
			if(nextBit == 1) {//if mode represent a match token
				nextIndex = in.read(BACK_WINDOW);//read match token structure
				nextLen = in.read(MAX_MATCH);
				//getting the 'nextLen' byte's, starting from 'nextIndex' at the back Buffer
				String toAdd = backBuffer.substring(nextIndex, nextIndex + nextLen);
				//write the byte sequences to the decoded file
				for(int i = 0; i < toAdd.length(); i++) {
					out.write(toAdd.charAt(i));
					backBuffer.append(toAdd.charAt(i));//update back window
				}
			}
			else {	
				//getting the next byte without decoding and write it to the decoded file
				nextIndex = in.read(8);
				if(nextIndex != -1)//if not end of file
					out.write(nextIndex);
				backBuffer.append((char)nextIndex);//update back window
			}
			if (backBuffer.length() > backWindowByts)//check window size and delete the beginning of the window if need 
	            backBuffer = backBuffer.delete(0, backBuffer.length() - backWindowByts);	
		}
		in.close();//close buffers
		out.close();
	}
	private void DeltaCodeCompress(String[] input_names, String[] output_names) throws IOException {
		//This function performs a Distance code algorithm, 
		//which is designed to improve the compression ratio by represent the file bytes by their numerical difference
		
		//Getting the file to encode and path to save the encoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream in = new BufferedInputStream(inFile);
		BufferedOutputStream out = new BufferedOutputStream(outFile);
		//create and set variable for the main loop
		//read and write the 1st byte to file, the decompress method will based on it
		int lastByte = in.read();
		int nextByte;
		out.write(lastByte);
		while(in.available() > 0) {
			//Getting the difference between the current byte and the next byte
			nextByte = in.read();
			int tmp = nextByte - lastByte;
			if(tmp < 0)
				tmp+=256;
			//write the new representation of the byte to the compressed file
			out.write(tmp);
			//update the current byte
			lastByte = nextByte;
		}
		in.close();
		out.close();
	}
	private void DeltaCodeDecompress(String[] input_names, String[] output_names) throws IOException {
		//Getting the file to decode and path to save the decoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream in = new BufferedInputStream(inFile);
		BufferedOutputStream out = new BufferedOutputStream(outFile);
		//getting and writing to output file the 1st byte that is on is original presentation
		int lastByte = in.read();
		int nextByte;
		out.write(lastByte);
		//encode the distance code from the file and write it to the uncoded file
		while(in.available() >0) {
			nextByte = (lastByte + in.read()) % 256;
			out.write(nextByte);
			lastByte = nextByte;
		}
		in.close();
		out.close();
	}
	private void MoveToFrontCompress(String[] input_names, String[] output_names) throws IOException {
		//This function performs a move to front algorithm, which is designed to improve the compression ratio
		//Getting the file to encode and path to save the encoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream in = new BufferedInputStream(inFile);
		BufferedOutputStream out = new BufferedOutputStream(outFile);
		//initial int[] to represent the ASCII table
		int[] bytesArray = new int [256];
		for(int i = 0; i < 256; i++)
			bytesArray[i] = i;
		int nextByte;
		//Perform move to front encode and write it to the encoded file
		while(in.available() > 0) {
			nextByte = in.read();
			int toWrite = moveByteToFront(nextByte, bytesArray);
			out.write(toWrite);
		}
		in.close();
		out.close();
	}
	private void MoveToFrontDecompress(String[] input_names, String[] output_names) throws IOException {
		//Getting the file to decode and path to save the decoded file
		FileInputStream inFile = new FileInputStream(input_names[0]);//files I/O streams and buffers
		FileOutputStream outFile = new FileOutputStream(output_names[0]);
		BufferedInputStream in = new BufferedInputStream(inFile);
		BufferedOutputStream out = new BufferedOutputStream(outFile);
		//initial int[] to represent the ASCII table
		int[] bytesArray = new int [256];
		for(int i = 0; i < 256; i++)
			bytesArray[i] = i;
		int nextByte;
		//Perform move to front decoder and write it to the uncompressed file
		while(in.available() > 0) {
			nextByte = in.read();
			int toWrite = moveIndexToFront(nextByte, bytesArray);
			out.write(toWrite);
		}
		in.close();
		out.close();
	}
	private int moveIndexToFront(int toMove, int[] bytesArray) {
		int temp = bytesArray[toMove];//this method update the moveToFront array for the decoder
		for(int i = toMove; i > 0; i--) 
			bytesArray[i] = bytesArray[i - 1];
		bytesArray[0] = temp;
		return temp;
	}
	private int moveByteToFront(int toMove, int[] bytesArray) {
		for(int i = 0; i < 256; i++) {//this method update the moveToFront array for the encoder
			if(bytesArray[i] == toMove) {
				for(int j = i - 1; j >= 0; j--)
					bytesArray[j+1] = bytesArray[j];
				bytesArray[0] = toMove;
				return i;
			}
		}
		return -1;
	}
	public static void main(String[] args) throws IOException {
		//This function manages the encoding/decoding process
		// input : A string[] named 'args' and contain at index : 
		/*		
		 * 		[0] - Input file path, file to encoding/decoding	
	
		 * 		[1] - Output file path, where to save the output after encoding/decoding
		
		 * 		[2] - Algorithm Mode :  True - encoding 
		 								False - decoding
		 								
		 * 		[3](only if [2]=true && [4]=false) - Move to Front Mode :  True - apply
		 									False - don't apply
		 
		 * 		[4](only if [2]=true && [3]=false) - Distance Code Mode :  True - apply
		 									False - don't apply		
		 
		 *		[5](only if [2]=true) - Sliding window size, in byte's 
				
		 *		[6](only if [2]=true) - Maximum Sequence to encode size, in bytes
		 
		 * 		[7]](only if [2]=true) - Minimum  Sequence to encode size, in bytes		
		 * */
		String[] inPath = {args[0]};
		String[] outPath = {args[1]};
		Boolean compress = Boolean.valueOf(args[2]);
		Compressor compressor;
		if(compress) {
			//Encode mode
			//Getting Compress settings
			Boolean moveToFront  = Boolean.valueOf(args[3]);
			Boolean distanceCode  = Boolean.valueOf(args[4]);
			int windowSize = (int)(Math.log(Integer.parseInt(args[5]))/Math.log(2)+1e-10);
			int maxLen = (int)(Math.log(Integer.parseInt(args[6]))/Math.log(2)+1e-10);
			int minLen = Integer.parseInt(args[7]);
			compressor = new Compressor(windowSize, maxLen, minLen);
			if(!moveToFront && !distanceCode)
				//only LZSS compression
				compressor.LZSS_compress(inPath,outPath);
			else if(moveToFront)
				//[3] - move to front - apply
				compressor.compressMoveToFront(inPath, outPath);
			else if(distanceCode)
				//[4] - distance code - apply
				compressor.compressDelta(inPath, outPath);
		}
		else {
			//Decode Mode
			//Getting file to decode
			BufferedInputStream buffer = new BufferedInputStream(new FileInputStream(inPath[0]));
			//The first 2 byte contain description about the input file compression : LZSS/LZSS + Move To Front / LZSS + Distance Code
			int lzssOnly = buffer.read();
			int mtf = buffer.read();
			//The back,front window's and the max sequence to decode length are read from the file
			compressor = new Compressor(0, 0, 0);
			if(lzssOnly == 0)
				//encode LZSS
				compressor.LZSS_decompress(inPath, outPath);
			else if(mtf == 0)
				//encode LZSS + Move To Front
				compressor.decompressMoveToFront(inPath, outPath);
			else
				//encode LZSS + Distance code
				compressor.decompressDelta(inPath, outPath);
			buffer.close();
		}
	}

}