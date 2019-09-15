package motor_package;

public class SerialReaderTester {

	public static void main(String[] args) {

		SerialReader reader = new SerialReader();
		
		String[] ports = reader.getPorts();
		
		for (int i = 0; i < ports.length; i++) {
		    System.out.println(ports[i]);
		}
		
	}
	
		
	
}