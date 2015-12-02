package db;

import java.io.IOException;

public class MyDatabase {

	public static void main(String[] args) {
		CLIController cli = new CLIController();
		try {
			cli.startCLI();
		} catch (IOException e) {
			System.err.println("IOException while executing the program: "+e.getMessage());
		}
	}

}
