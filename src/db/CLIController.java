package db;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CLIController {
	boolean shutDown;
	
	public void startCLI() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(!shutDown){
			System.out.print("prompt> ");
			String ip = null;
			String line="";
			while((ip=br.readLine())!=null){
				line=line+ip;
				if(ip.contains(";")||ip.contains("quit"))
					break;
				else
					line = line+" ";
				System.out.print("prompt> ");
			}
			line = formatLine(line);
			if(line.startsWith("quit")){
				shutDown = true;
			}
			else if(line.startsWith("select")){
				System.out.println("Evaluating SELECT query");
				evaluateSelect(line);
			}
			else if(line.startsWith("import")){
				System.out.println("Evaluating IMPORT query");
				evaluateImport(line);
			}
			else if(line.startsWith("delete")){
				System.out.println("Evaluating DELETE query");
				System.out.println(line);
			}
			else if(line.startsWith("insert")){
				System.out.println("Evaluating INSERT query");
				System.out.println(line);
			}
		}
	}

	private void evaluateSelect(String command) {
		String select = command.substring(command.indexOf("select")+6, command.indexOf(" from ")).trim();
//		String[] items = select.split(",");
		String dbname = command.substring(command.indexOf(" from ")+6, command.indexOf(";")).trim();
		String clause = null;
		if(dbname.contains(" where ")){
			clause = dbname.substring(dbname.indexOf(" where ")+7);
			dbname = dbname.substring(0, dbname.indexOf(" where ")).trim();
		}
		System.out.println("Executing: SELECT "+select+" FROM "+dbname+" WHERE "+clause+";");
	}

	private void evaluateImport(String command) {
		String fileName = command.substring(command.indexOf("import")+6, command.indexOf(";")).trim();
		System.out.println("Importing from file: "+fileName);
		String fn=fileName;
		if(fileName.contains("."))
			fn = fileName.substring(0, fileName.lastIndexOf("."));
		File file = new File("PHARMA_TRIALS_1000B.csv");
		String[] headers = null;
		int lines = 0;
		String line="";
		try (BufferedReader br = new BufferedReader(new FileReader(file)); 
				DataOutputStream dos = new DataOutputStream(new FileOutputStream(fn+".bin"))) {
			while((line = br.readLine())!=null){
				lines++;
				
				String[] split = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				if(lines ==1){
					headers = new String[split.length];
					for (int i = 0; i < split.length; i++) {
						headers[i] = split[i];
					}
					continue;
				}
				//needs to be hashed //id
				dos.writeInt(Integer.parseInt(split[0]));
				dos.flush();
				
				
				//needs to be hashed //company
				String companyName = split[1];
				if(companyName.startsWith("\"") && companyName.endsWith("\""))
					companyName = companyName.substring(1, companyName.length()-1);
				byte length = (byte) companyName.length();
				dos.writeByte(length);
				dos.flush();
				dos.writeBytes(companyName);
				dos.flush();
				
				//needs to be hashed //drug_id
				dos.writeBytes(split[2]);
				dos.flush();
				
				//needs to be hashed //trials
				dos.writeShort(Integer.parseInt(split[3]));
				dos.flush();
				//needs to be hashed //patients
				dos.writeShort(Integer.parseInt(split[4]));
				dos.flush();
				//needs to be hashed //dosage_mg
				dos.writeShort(Integer.parseInt(split[5]));
				dos.flush();
				
				//needs to be hashed //reading
				dos.writeFloat(Float.parseFloat(split[6]));
				dos.flush();
				
				byte lastbyte = 0;
				if(split[7].equals("true"))
					lastbyte = (byte) (lastbyte | 0x08);
				if(split[8].equals("true"))
					lastbyte = (byte) (lastbyte | 0x04);
				if(split[9].equals("true"))
					lastbyte = (byte) (lastbyte | 0x02);
				if(split[10].equals("true"))
					lastbyte = (byte) (lastbyte | 0x01);
				dos.writeByte(lastbyte);
				dos.flush();
			}
			System.out.println("Number of records imported = "+(lines-1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String formatLine(String line) {
		return line.toLowerCase();
	}

}
