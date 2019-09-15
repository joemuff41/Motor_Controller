package motor_package;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;
import com.fazecast.jSerialComm.*;



public class SerialReader {

	private byte[] clearPortArray;
	SerialPort ports[];
	
	public SerialReader() {
		
		ports = SerialPort.getCommPorts();
		clearPortArray = new byte[10000];
	}
	
	public String[] getPorts() {

		String[] portNames = new String[ports.length];
		
		for (int i = 0; i < ports.length; i++) {
			portNames[i] = ports[i].getSystemPortName();
		}
		
		return  portNames;
	}
	
	
	public boolean connectPort(String aPortName) {
		
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				if (port.openPort()) {
					port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
					port.setBaudRate(9600);
					return true;
				}
				
			}
		}
		
		return false;
	}
	
	public boolean disconnectPort(String aPortName) {
		
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				if (port.closePort()) {
					return true;
				}
				
			}
		}
		
		return false;
	}
	
	/*
	 * @return Whether the port with aPortName has sent any message
	 */
	public boolean hasMessage(String aPortName) {
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				return port.bytesAvailable() > 0;				
			}
		}
		
		return false;
	}
	
	/*
	 * Reads a message from the port with port name aPortName
	 * 
	 * @param aPortName		The name of the port to read from
	 * @param buffer		The buffer to fill with the message
	 * @param bytesToRead	The number of bytes to read from the port
	 * 
	 * @return				The number of bytes successfully read, or -1 if there was an error reading from the port
	 */
	public int readMessage(String aPortName, byte[] buffer, long bytesToRead) {
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				return port.readBytes(buffer, bytesToRead);
			}
		}
		
		return -1;
	}
	
	/*
	 * Writes a message to the port with port name aPortName
	 * 
	 * @param aPortName		The name of the port to write to
	 * @param buffer		The buffer to send
	 * @param bytesToRead	The number of bytes to write to the port
	 * 
	 * @return				The number of bytes successfully written, or -1 if there was an error writing to the port
	 */
	public int writeMessage(String aPortName, byte[] buffer, long bytesToWrite) {
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				return port.writeBytes(buffer, bytesToWrite);
			}
		}
		
		return -1;
	}
	
	/*
	 * Reads all the available bytes from the port with name aPortName
	 */
	public void clearPort(String aPortName) {
		for (SerialPort port : ports) {
			if (port.getSystemPortName().equalsIgnoreCase(aPortName)) {
				port.readBytes(clearPortArray, port.bytesAvailable());
			}
		}
	}
	
}
