/*
 * Matan Danino: 304802887
 * Shir Elbaz: 204405690
 * Gal Arus: 204372619
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.BitSet;
//this class read\write bits from\to file
class BitBuffer{
	final BufferedOutputStream OUT_STREAM;
	final BufferedInputStream INPUT_STREAM;
	int _outPowerCounter;
	int _outBuffer;
	int _inBuffer;
	int _inPowerCounter;
	BitSet _bitsBuffer;
	//constructor get buffered out\in stream' (set one of them to null)
	BitBuffer(BufferedOutputStream out ,BufferedInputStream in){
		OUT_STREAM = out;
		INPUT_STREAM = in;
		_outPowerCounter = 7;//init buffer variables
		_outBuffer = 0;
		_inPowerCounter = 8;
		_bitsBuffer = new BitSet(8);
	}
	//return how many bytes available
	public int length() throws IOException {
		return INPUT_STREAM.available();
	}
	//get binary string and write the bits in the string
	public void write(String bits) throws IOException {
		for(int i = 0; i < bits.length(); i++, _outPowerCounter--) {//for each bit update counter
			if(_outPowerCounter == -1)//if buffet is full
				clearBuffer();
			if(bits.charAt(i) == '1')//if need to update value
				_outBuffer+=Math.pow(2, _outPowerCounter);
		}
	}
	//get integer number and size in bits and write the binary value to the buffer 
	public void write(int toBits, int howManyBits) throws IOException {
		if(toBits < 0)
			toBits+=256;
		String str = Integer.toBinaryString(toBits);//converts to binary string
		while(str.length() < howManyBits)//add the miss bits
			str = "0" + str;
		write(str);//call to method write
	}
	//this method write the last bits in the buffer to the file and close the buffer
	public int close() throws IOException {
		if(_outPowerCounter != 7)//if buffer not empty
			OUT_STREAM.write(_outBuffer);
		if(OUT_STREAM != null)//close the buffer
			OUT_STREAM.close();
		return _outPowerCounter + 1;// return how many bits was add to the last byte
	}
	//this method get integer number that represent how many bits need to read from file and return the integer value of them
	public int read(int howManyBits) throws IOException {
		int bitsCalc = 0;//sum of bits
		while(howManyBits > 0) {//while need to read more
			if(_inPowerCounter == 8)//if buffer is empty
				if(fillBuffer() == -1)//fill buffer and check if end of file
					return -1;//end of file
			if(_bitsBuffer.get(_inPowerCounter))//if bit == 1
				bitsCalc+=Math.pow(2, howManyBits - 1);//add to sum
			_inPowerCounter++;//update counters
			howManyBits--;
		}
		return bitsCalc;//return bits value
	}
	//this method fill the bits buffer
	private int fillBuffer() throws IOException {
		_bitsBuffer.clear();//set bits to zero
		int next = INPUT_STREAM.read();//read next byte
		if (next == -1)//if end of file return -1 flag
			return -1;
		for(int i = 7 ; i > -1 ; i--, next/=2) {//for each bit in the byte
			if(next % 2 == 1)//update to one if need to
				_bitsBuffer.set(i);
		}
		_inPowerCounter = 0;//update counter
		return 1;//return flag	
	}
	//this method write the byte in the buffer to the file and update counters
	private void clearBuffer() throws IOException {
		OUT_STREAM.write(_outBuffer);
		_outPowerCounter = 7;
		_outBuffer = 0;
	}
}