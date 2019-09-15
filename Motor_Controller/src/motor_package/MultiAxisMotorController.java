package motor_package;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

public class MultiAxisMotorController {

	static final int VALUE_LENGTH = 4;		//Length of a message value in bytes
	private SerialReader[] readers;
	private byte[][] confirmedMove;
	private int numMotors;
	private String[] motorNames;
	private boolean[] motorIsBusy;
	
	//Types of orders in the serial communication
	public enum Order {
		END					((byte) 0x1),
		HELLO				((byte) 0x2),
		CONNECTED			((byte) 0x3),
		ERROR_MESSAGE		((byte) 0x4),
		MOVE_POSITIVE		((byte) 0x5),
		MOVE_NEGATIVE		((byte) 0x6);
		
		private final byte orderCode;
		
		private Order(byte orderCode) {
			this.orderCode = orderCode;
		}
		
		public byte getOrderCode() {
			return this.orderCode;
		}
	}
	
	/**
	 * 
	 * @param aNumMotors		The number of motors to control
	 * @param aMotorNames		The serial port names of the motors. Must be a string array of length >= aNumMotors
	 */
	public MultiAxisMotorController(int aNumMotors, String[] aMotorNames) {
		numMotors = aNumMotors;
		motorNames = new String[numMotors];
		readers = new SerialReader[numMotors];
		confirmedMove = new byte[numMotors][VALUE_LENGTH + 2];
		motorIsBusy = new boolean[numMotors];
		
		//Get all the port names and connect to the various ports
		for (int i = 0; i < numMotors; i++) {
			motorNames[i] = aMotorNames[i];
			readers[i] = new SerialReader();
			for (int j = 0; j < VALUE_LENGTH + 2; j++) {
				confirmedMove[i][j] = (byte) 0x00;
			}
			motorIsBusy[i] = false;
			connectMotor(i);
		}
			
		
	}
	
	/**
	 * 
	 * @param aMotorNumber			The number of the motor to connect to (corresponds to the order listed in the constructor)
	 * @return						Whether the connection was successful or not
	 */
	private boolean connectMotor(int aMotorNumber) {
		
		byte[] helloReceived = new byte[2];
		
		//Connect the port for the motor
		boolean connected = readers[aMotorNumber].connectPort(motorNames[aMotorNumber]);
		System.out.println("Connected to port " + motorNames[aMotorNumber] + ": " + connected);
		writeMessage(aMotorNumber, Order.HELLO);
			
		while (!readers[aMotorNumber].hasMessage(motorNames[aMotorNumber])) {
			//System.out.println("No message received, waiting 1 second then saying hello again");
			pause(50);
			writeMessage(aMotorNumber, Order.HELLO);
		}
			
		//Read 10 hellos from the motor controller
		for (int j = 0; j < 10; j++) {
			while (!readers[aMotorNumber].hasMessage(motorNames[aMotorNumber])) {
				pause(50);
			}
			readers[aMotorNumber].readMessage(motorNames[aMotorNumber], helloReceived, 2);
	 		//String s1 = String.format("%8s", Integer.toBinaryString(helloReceived[0] & 0xFF)).replace(' ', '0');
			//System.out.println("Motor " + i + " says: " + s1);
		}
			
		if (helloReceived[0] == Order.HELLO.getOrderCode()) {
			//Tell the motor controller that we are connected
			writeMessage(aMotorNumber, Order.CONNECTED);
		}
			
		readers[aMotorNumber].clearPort(motorNames[aMotorNumber]);
		System.out.println("Connected to motor " + motorNames[aMotorNumber]);
		
		return connected;
	}
	
	public String[] getAvailablePortNames() {
		return readers[0].getPorts();
	}
	
	
	/**
	 * Moves the motor by the last number of rotations set by setNumRotations
	 * @param aMotorNumber		The motor number to move (corresponds with the order of the serial ports given in the constructor)
	 * @param aNumRotations		The number of rotations to move
	 */
	public void moveMotor(int aMotorNumber, double aNumRotations) {
        Thread t = new Thread(new Runnable() {           
            public void run() { 
            	motorIsBusy[aMotorNumber] = true;
            	this.pause(50);
        		readers[aMotorNumber].clearPort(motorNames[aMotorNumber]);
        		long degreesToMove = (long) Math.abs(aNumRotations * 360);
        		if (aNumRotations > 0) {
        			writeMessage(aMotorNumber, Order.MOVE_POSITIVE, degreesToMove);
        		}
        		else {
        			writeMessage(aMotorNumber, Order.MOVE_NEGATIVE, degreesToMove);
        		}
        		
        		this.pause(50);
        		byte[] errorCheck = new byte[2];
        		readers[aMotorNumber].readMessage(motorNames[aMotorNumber], errorCheck, 2);
        		boolean validMessageReceived = false;
        		while (!validMessageReceived) {
        			if (!(errorCheck[0] == Order.ERROR_MESSAGE.orderCode 
        					|| errorCheck[0] == Order.MOVE_NEGATIVE.orderCode || errorCheck[0] == Order.MOVE_POSITIVE.orderCode)) {
        				
        				readers[aMotorNumber].readMessage(motorNames[aMotorNumber], errorCheck, 2);				
        			}
        			else {
        				validMessageReceived = true;
        			}
        		}
        		
        		//Check if the motor controller received the right message
        		if (errorCheck[0] == Order.ERROR_MESSAGE.orderCode) {
        			//If didn't receive the right message, clear the port and send it again
        			System.out.println("There was an error...resyncing " + motorNames[aMotorNumber]);
        			writeMessage(aMotorNumber, Order.ERROR_MESSAGE);
        			this.pause(50);
        			readers[aMotorNumber].clearPort(motorNames[aMotorNumber]);
        			if (aNumRotations > 0) {
        				writeMessage(aMotorNumber, Order.MOVE_POSITIVE, degreesToMove);
        			}
        			else {
        				writeMessage(aMotorNumber, Order.MOVE_NEGATIVE, degreesToMove);
        			}
        			
        			pause(50);
        			readers[aMotorNumber].readMessage(motorNames[aMotorNumber], confirmedMove[aMotorNumber], VALUE_LENGTH);
        		}
        		else {
        			//If it did receive the right message, read the rest of the message
        			readers[aMotorNumber].readMessage(motorNames[aMotorNumber], confirmedMove[aMotorNumber], VALUE_LENGTH);
        			for (int i = 0; i < VALUE_LENGTH; i++) {
        				confirmedMove[aMotorNumber][VALUE_LENGTH + 1 - i] = confirmedMove[aMotorNumber][VALUE_LENGTH - 1 - i];
        			}
        			confirmedMove[aMotorNumber][0] = errorCheck[0];
        			confirmedMove[aMotorNumber][1] = errorCheck[1];
        		}

        		System.out.println("Motor " + motorNames[aMotorNumber] + " did this: " + confirmedMove[aMotorNumber][0]);
        		
        		byte[] receivedMove = new byte[VALUE_LENGTH];
        		for (int i = 0; i < VALUE_LENGTH; i++) {
        			receivedMove[i] = confirmedMove[aMotorNumber][i+1];
        		}
        		ByteBuffer bb = ByteBuffer.wrap(receivedMove);
        		bb.order(ByteOrder.LITTLE_ENDIAN);
        		System.out.println("Motor " + motorNames[aMotorNumber] + " is moving this many degrees: " + bb.getInt());
        		motorIsBusy[aMotorNumber] = false;
            } 
            
        	/*
        	 * Pauses the thread for a given number of milliseconds
        	 */
        	private void pause(int milliseconds) {
        		try {
        			Thread.sleep(milliseconds);
        		} catch (InterruptedException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        	}
        });
        
        t.start();
    }
	
	/**
	 * Returns whether the motor with number aMotorNumber is busy
	 * @param aMotorNumber			The motor to check
	 * @return						whether the motor with number aMotorNumber is busy
	 */
	public boolean isBusy(int aMotorNumber) {
		return motorIsBusy[aMotorNumber];
	}
	
	/*
	 * Uses the serial reader object to write a message to port with order aOrder (simple orders, i.e. HELLO, CONNECTED)
	 */
	private void writeMessage(int aMotorNumber, Order aOrder) {
		byte[] message = new byte[] {aOrder.getOrderCode(), Order.END.getOrderCode()};
		
		readers[aMotorNumber].writeMessage(motorNames[aMotorNumber], message, 2);
	}
	
	/*
	 * Uses the serial reader object to write a message with order aOrder and value (for MOVE_POSITIVE, MOVE_NEGATIVE)
	 * Writes a 32 bit unsigned int. The long parameter is truncated to a 32 bit unsigned int
	*/
	private void writeMessage(int aMotorNumber, Order aOrder, long value) {
		byte[] message = new byte[VALUE_LENGTH + 2];
		
		byte[] longByteArray = longToBytes(value);
		
		message[0] = aOrder.getOrderCode();
		
		for (int i = 0; i < VALUE_LENGTH; i++) {
			message[i + 1] = longByteArray[i];
		}
		
		message[VALUE_LENGTH + 1] = Order.END.getOrderCode();
		
		readers[aMotorNumber].writeMessage(motorNames[aMotorNumber], message, VALUE_LENGTH + 2);
	}
	
	public static byte[] longToBytes(long l) {
	    byte[] result = new byte[8];
	    for (int i = 0; i < 8; i++) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}
	
	/*
	 * Pauses for a given number of milliseconds
	 */
	private void pause(int milliseconds) {
		try {
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
