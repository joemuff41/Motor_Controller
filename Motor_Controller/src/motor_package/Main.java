package motor_package;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		
		MultiAxisMotorController controller = new MultiAxisMotorController(2, new String[] {"COM3", "COM9"});
		
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/**
		
		controller.moveMotor(0, -10);
		controller.moveMotor(1, -10);
		
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		controller.moveMotor(0, 10);
		controller.moveMotor(1, 10);
	
		*/
		
		
		Scanner input = new Scanner(System.in);
		
		final int NUM_MOTORS = 2;
		
		boolean done = false;
		String[] command;
		int motorNumber = 0;
		double numRotations = 0;
		
		
		while (!done) {
			
			System.out.println("Enter a command - message syntax is: motor_number,number_of_rotations");
			
			command = input.nextLine().split(",");
			
			if (command.length != 2) {
				
				if (command.length == 1 && command[0].equalsIgnoreCase("Q")) {
					System.out.println("Program terminating");
					break;
				}
				
				System.out.println("Invalid number of arguments - message syntax is: motor_number,number_of_rotations");
				continue;
			}
			
			try {
				motorNumber = Integer.parseInt(command[0]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid motor number argument - message syntax is: motor_number,number_of_rotations");
				continue;
			}
			
			if (motorNumber < 0 || motorNumber > NUM_MOTORS) {
				System.out.println("There are " + NUM_MOTORS + " motors");
				continue;
			}
			
			try {
				numRotations = Double.parseDouble(command[1]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid number of rotations argument - message syntax is: motor_number,number_of_rotations");
				continue;
			}
			
			if (controller.isBusy(motorNumber)) {
				System.out.println("Motor " + motorNumber + " is busy. Try again later");
				continue;
			}
			
			controller.moveMotor(motorNumber, numRotations);
		}
		
	}
	
		
	
}
