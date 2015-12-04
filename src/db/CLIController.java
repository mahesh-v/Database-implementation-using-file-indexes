package db;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import data.WhereClause;
import data.PharmaDatabase;

public class CLIController {
	private static final String PROMPT_STRING = "prompt> ";
	private static final String ENCODING = "UTF-8";
	private static final String BINARY_EXTENSION = ".db";
	boolean shutDown;
	private static ArrayList<PharmaDatabase> dbList = new ArrayList<PharmaDatabase>();
	
	public void startCLI() throws IOException {
		loadExistingDatabases();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(!shutDown){
			System.out.print(PROMPT_STRING);
			String ip = null;
			String line="";
			while((ip=br.readLine())!=null){
				line=line+ip;
				if(ip.contains(";")||ip.contains("quit")||ip.contains("exit"))
					break;
				else
					line = line+" ";
				System.out.print(PROMPT_STRING);
			}
			line = formatLine(line);
			if(line.startsWith("quit")||line.startsWith("exit")){
				shutDown = true;
			}
			else if(line.startsWith("select")){
				long nano = System.nanoTime();
				System.out.println("Evaluating SELECT query");
				if(evaluateSelect(line))
					System.out.println("Command executed in "+((System.nanoTime() - nano)/1E6)+"ms");
			}
			else if(line.startsWith("import")){
				long nano = System.nanoTime();
				System.out.println("Evaluating IMPORT query");
				if(evaluateImport(line))
					System.out.println("Command executed in "+((System.nanoTime() - nano)/1E6)+"ms");
			}
			else if(line.startsWith("delete")){
				long nano = System.nanoTime();
				System.out.println("Evaluating DELETE query");
				if(evaluateDelete(line))
					System.out.println("Command executed in "+((System.nanoTime() - nano)/1E6)+"ms");
			}
			else if(line.startsWith("insert")){
				long nano = System.nanoTime();
				System.out.println("Evaluating INSERT query");
				if(evaluateInsert(line))
					System.out.println("Command executed in "+((System.nanoTime() - nano)/1E6)+"ms");
			}
		}
	}

	private boolean evaluateInsert(String command) {
		if(!command.contains(" into ")||!command.contains(" values ")){
			System.out.println("Invalid insert syntax");
			System.out.println("Please enter in format: INSERT INTO <dbname> VALUES (<comma seperated values>);");
			return false;
		}
		String dbName = command.substring(command.indexOf(" into ")+6, command.indexOf(" values ")).trim();
		PharmaDatabase db = getDatabase(dbName);
		if(db==null){
			System.out.println("No such database found. Unable to complete insert operation.");
			return false;
		}
		String values_cs = command.substring(command.indexOf("(")+1, command.indexOf(")"));
		File dbFile = new File(db.getDbName()+BINARY_EXTENSION);
		try(DataOutputStream dos = new DataOutputStream(new FileOutputStream(dbFile, true))){
			writeLineToDbAndFile(db, values_cs, dos, dbFile.length());
		} catch (IOException e) {
			System.err.println("IOException while writing to Database: "+e.getMessage());
			return false;
		}
		writeIndexMapsToFiles(db);
		return true;
	}

	private boolean evaluateDelete(String command) {
		if(!command.contains(" from ")||!command.contains(" where ")){
			System.out.println("Invalid delete syntax");
			System.out.println("Please enter in format: DELETE FROM <dbname> WHERE <condition>;");
			return false;
		}
		String dbname = command.substring(command.indexOf(" from ")+6, command.indexOf(" where "));
		String clause = command.substring(command.indexOf(" where ")+7, command.indexOf(";"));
		PharmaDatabase db = getDatabase(dbname);
		if(db==null){
			System.out.println("No such database exists.");
			return false;
		}
		Map<Float, ArrayList<Long>> locationMap = evaluateClauseAndGetMap(db, clause);
		try (RandomAccessFile raf = new RandomAccessFile(db.getDbName()+BINARY_EXTENSION, "rw")){
			for (Float id : locationMap.keySet()) {
				ArrayList<Long> memLocations = locationMap.get(id);
				for (Long loc : memLocations) {
					raf.seek(loc);
					raf.skipBytes(4);//for int id
					byte compLength = raf.readByte();
					raf.skipBytes(compLength+16);//length of company+6drug_id+(2*3 short)+(4float)
					long location = raf.getFilePointer();
					byte lastByte = raf.readByte();
					byte newByte = (byte) (lastByte | (1L<<7));
					raf.seek(location);
					raf.writeByte(newByte);
				}
			}
		} catch (IOException e) {
			System.err.println("IOException while deleting record from database: "+e.getMessage());
			return false;
		}
		return true;
	}

	private boolean evaluateSelect(String command) {
		if(!command.contains(" from ")){
			System.out.println("Incorrect select syntax.");
			System.out.println("Please enter in format: SELECT <identifiers> FROM <dbname> [WHERE <condition>];");
			return false;
		}
		String select = command.substring(command.indexOf("select")+6, command.indexOf(" from ")).trim();
		
		String dbname = command.substring(command.indexOf(" from ")+6, command.indexOf(";")).trim();
		String clause = null;
		if(dbname.contains(" where ")){
			clause = dbname.substring(dbname.indexOf(" where ")+7);
			dbname = dbname.substring(0, dbname.indexOf(" where ")).trim();
		}
		String[] items = select.split(",");
		PharmaDatabase db = getDatabase(dbname);
		if(db==null){
			System.out.println("No such database exists.");
			return false;
		}
		boolean star = false;
		if(select.equals("*"))
			star=true;
		Map<Float, ArrayList<Long>> locationMap=null;
		if(clause!=null)
			locationMap =  evaluateClauseAndGetMap(db, clause);
		else
			locationMap = db.getIdMap();
		if(locationMap==null){
			System.out.println("Unable to evaluate WHERE clause.");
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(db.getDbName()+BINARY_EXTENSION, "r")){
				
			displayRecord(items, star, "id",
					"company", "drug_id",
					"trials", "patients",
					"dosage_mg", "reading",
					"double_blind", "controlled_study",
					"govt_funded", "fda_approved");
			int numDisplayed = 0;
			for (Float id : locationMap.keySet()) {
				ArrayList<Long> memLocations = locationMap.get(id);
				for (Long loc : memLocations) {
					raf.seek(loc);
					int id_display = raf.readInt();
					byte compLength = raf.readByte();
					byte[] b = new byte[compLength];
					raf.read(b);
					String companyName_display = new String(b, ENCODING);
					byte[] b2 = new byte[6];
					raf.read(b2);
					String drug_id_display = new String(b2, ENCODING);
					short trials_display = raf.readShort();
					short patients_display = raf.readShort();
					short dosage_mg_display = raf.readShort();
					float reading_display = raf.readFloat();
					byte lastByte = raf.readByte();
					boolean deleted = (lastByte&(1L<<7))==128;
					boolean double_blind = (lastByte&(1L<<3))==8;
					boolean controlled_study = (lastByte&(1L<<2))==4;
					boolean govt_funded = (lastByte&(1L<<1))==2;
					boolean fda_approved = (lastByte&(1L))==1;
					
					if(!deleted)
					{
						displayRecord(items, star, id_display+"",
								companyName_display, drug_id_display,
								trials_display+"", patients_display+"",
								dosage_mg_display+"", reading_display+"",
								double_blind+"", controlled_study+"",
								govt_funded+"", fda_approved+"");
						numDisplayed++;
					}
				}
			}
			if(numDisplayed == 0)
				System.out.println("No records found for given select condition.");
			else
				System.out.println("Displaying "+numDisplayed+" records.");
			
		} catch (IOException e) {
			System.err.println("IOException when reading from database: "+e.getMessage());
			return false;
		}
		return true;
	}
	
	private boolean evaluateImport(String command) {
		String fileName = command.substring(command.indexOf("import")+6, command.indexOf(";")).trim();
		String fullPath = fileName;
		if(fileName.contains(File.separator))
			fileName = fileName.substring(fileName.lastIndexOf(File.separator)+1);
		String alias=null;
		if(fileName.contains(" as ")){
			alias = fileName.substring(fileName.indexOf(" as ")+4).trim();
			fullPath = fullPath.substring(0, fullPath.indexOf(" as ")).trim();
		}
		if(fileName.startsWith("\""))
			fileName = fileName.substring(1, fileName.length()-1);
		if(fileName.endsWith("\""))
			fileName = fileName.substring(0, fileName.length()-1);
		if(alias ==null){
			if(fileName.contains("."))
				alias = fileName.substring(0, fileName.lastIndexOf("."));
			else
				alias = fileName;
		}
		if(fullPath.startsWith("\""))
			fullPath = fullPath.substring(1, fullPath.length()-1);
		if(fullPath.endsWith("\""))
			fullPath = fullPath.substring(0, fullPath.length()-1);
		fullPath = fullPath.trim();
		
		System.out.println("Importing from file: "+fullPath);
		PharmaDatabase db = getDatabase(alias);
		if(db!=null){
			System.out.println("Database already exists with that name. Please use a different file, or alias \"as\" to name it.");
			return false;
		}
		else
			db = new PharmaDatabase();
		db.setDbName(alias);
		File file = new File(fullPath);
		int lines = 0;
		String line="";
		try (BufferedReader br = new BufferedReader(new FileReader(file)); 
				DataOutputStream dos = new DataOutputStream(new FileOutputStream(alias+BINARY_EXTENSION))) {
			while((line = br.readLine())!=null){
				lines++;
				
				if(lines ==1){
					continue;
				}
				writeLineToDbAndFile(db, line, dos, dos.size());
			}
			System.out.println("Number of records imported = "+(lines-1));
		} catch (IOException e) {
			System.err.println("IOException when writing to database: "+e.getMessage());
			return false;
		}
		writeIndexMapsToFiles(db);
		dbList.add(db);
		return true;
	}

	private void displayRecord(String[] items, boolean star, String id_display,
			String companyName_display, String drug_id_display,
			String trials_display, String patients_display,
			String dosage_mg_display, String reading_display,
			String double_blind, String controlled_study,
			String govt_funded, String fda_approved) {
		if(star || isContained("id", items))
			System.out.print(id_display+"\t|");
		if(star || isContained("company", items))
			System.out.print(companyName_display+"\t|");
		if(star || isContained("drug_id", items))
			System.out.print(drug_id_display+"|");
		if(star || isContained("trials", items))
			System.out.print(trials_display+"|");
		if(star || isContained("patients", items))
			System.out.print(patients_display+"|");
		if(star || isContained("dosage_mg", items))
			System.out.print(dosage_mg_display+"|");
		if(star || isContained("reading", items))
			System.out.print(reading_display+"|");
		if(star || isContained("double_blind", items))
			System.out.print(double_blind+"|");
		if(star || isContained("controlled_study", items))
			System.out.print(controlled_study+"|");
		if(star || isContained("govt_funded", items))
			System.out.print(govt_funded+"|");
		if(star || isContained("fda_approved", items))
			System.out.print(fda_approved+"|");
		System.out.println();
	}

	private Map<Float, ArrayList<Long>> evaluateClauseAndGetMap( PharmaDatabase db, String clause) 
	{
		WhereClause c = new WhereClause(clause);
		TreeMap<Float, ArrayList<Long>> map = getMapBasedOnClause(c.clauseVariable, db);
		if(map==null)
			return null;
		if(c.str_val == null){
			if(c.equalTo){
				if(c.greaterThan)
					return map.tailMap(c.flt_val, true);
				else if(c.lesserThan)
					return map.headMap(c.flt_val, true);
				else if(c.not){
					TreeMap<Float, ArrayList<Long>> not_map = new TreeMap<Float, ArrayList<Long>>();
					not_map.putAll(map);
					not_map.remove(c.flt_val);
					return not_map;
				}
				else{
					TreeMap<Float, ArrayList<Long>> equalTo_map = new TreeMap<Float, ArrayList<Long>>();
					ArrayList<Long> value = map.get(c.flt_val);
					if(value!=null)
						equalTo_map.put(c.flt_val, value);
					return equalTo_map;
				}
			}
			if(c.greaterThan){
				return map.tailMap(c.flt_val, false);
			}
			if(c.lesserThan){
				return map.headMap(c.flt_val, false);
			}
		}
		else{//for company, drug id, and all boolean values
			float hashcode = c.str_val.hashCode();
			if(c.equalTo&&!c.not){//equal to
				TreeMap<Float, ArrayList<Long>> equalTo_map = new TreeMap<Float, ArrayList<Long>>();
				ArrayList<Long> value = map.get(hashcode);
				if(value!=null)
					equalTo_map.put(hashcode, value);
				return equalTo_map;
			}
			if(c.equalTo&&c.not){//not equal to
				TreeMap<Float, ArrayList<Long>> not_map = new TreeMap<Float, ArrayList<Long>>();
				not_map.putAll(map);
				not_map.remove(hashcode);
				return not_map;
			}
		}
		return null;
	}

	private TreeMap<Float, ArrayList<Long>> getMapBasedOnClause(String clauseVariable, PharmaDatabase db) 
	{
		if(clauseVariable.equalsIgnoreCase("id"))
			return db.getIdMap();
		else if(clauseVariable.equalsIgnoreCase("company"))
			return db.getCompanyMap();
		else if(clauseVariable.equalsIgnoreCase("drug_id"))
			return db.getDrugIdMap();
		else if(clauseVariable.equalsIgnoreCase("trials"))
			return db.getTrialsMap();
		else if(clauseVariable.equalsIgnoreCase("patients"))
			return db.getPatientsMap();
		else if(clauseVariable.equalsIgnoreCase("dosage_mg"))
			return db.getDosageMap();
		else if(clauseVariable.equalsIgnoreCase("reading"))
			return db.getReadingMap();
		else if(clauseVariable.equalsIgnoreCase("double_blind"))
			return db.getDoubleBlindMap();
		else if(clauseVariable.equalsIgnoreCase("controlled_study"))
			return db.getControlledStudyMap();
		else if(clauseVariable.equalsIgnoreCase("govt_funded"))
			return db.getGovt_fundedMap();
		else if(clauseVariable.equalsIgnoreCase("fda_approved"))
			return db.getFda_approvedMap();
		else
			return null;
	}

	private boolean isContained(String attribute, String[] items) {
		for (String item : items) {
			if(item.trim().equalsIgnoreCase(attribute))
				return true;
		}
		return false;
	}

	
	
	private void writeLineToDbAndFile(PharmaDatabase db, String line, DataOutputStream dos, long location) throws IOException{
		String[] split = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		for (int i = 0; i < split.length; i++) {
			split[i] = split[i].trim();
		}
		//id
		int id = Integer.parseInt(split[0]);
		Float id_key = new Float(split[0]);
		if(!db.getIdMap().containsKey(id_key))
			db.getIdMap().put(id_key, new ArrayList<Long>());
		else{
			System.out.println("Unable to insert record: "+line);
			System.out.println("Duplicate primary key id="+id);
			return;
		}
		db.getIdMap().get(id_key).add(location);
		
		dos.writeInt(id);
		dos.flush();
		
		//company
		String companyName = split[1];
		if(companyName.startsWith("\"") && companyName.endsWith("\""))
			companyName = companyName.substring(1, companyName.length()-1);
		byte length = (byte) companyName.length();
		dos.writeByte(length);
		dos.flush();
		dos.writeBytes(companyName);
		dos.flush();
		float cmpHash = companyName.toLowerCase().hashCode();
		if(!db.getCompanyMap().containsKey(cmpHash))
			db.getCompanyMap().put(cmpHash, new ArrayList<Long>());
		db.getCompanyMap().get(cmpHash).add(location);
		
		
		//drug_id
		dos.writeBytes(split[2]);
		dos.flush();
		float drugidHash = split[2].toLowerCase().hashCode();
		if(!db.getDrugIdMap().containsKey(drugidHash))
			db.getDrugIdMap().put(drugidHash, new ArrayList<Long>());
		db.getDrugIdMap().get(drugidHash).add(location);
		
		//trials
		int trials = Integer.parseInt(split[3]);
		dos.writeShort(trials);
		dos.flush();
		if(!db.getTrialsMap().containsKey((float) trials))
			db.getTrialsMap().put((float) trials, new ArrayList<Long>());
		db.getTrialsMap().get((float)trials).add(location);
		
		//patients
		int patients = Integer.parseInt(split[4]);
		dos.writeShort(patients);
		dos.flush();
		if(!db.getPatientsMap().containsKey((float) patients))
			db.getPatientsMap().put((float) patients, new ArrayList<Long>());
		db.getPatientsMap().get((float) patients).add(location);
		
		//dosage_mg
		int dosage_mg = Integer.parseInt(split[5]);
		dos.writeShort(dosage_mg);
		dos.flush();
		if(!db.getDosageMap().containsKey((float) dosage_mg))
			db.getDosageMap().put((float) dosage_mg, new ArrayList<Long>());
		db.getDosageMap().get((float) dosage_mg).add(location);
		
		//reading
		float reading = Float.parseFloat(split[6]);
		dos.writeFloat(reading);
		dos.flush();
		if(!db.getReadingMap().containsKey(reading))
			db.getReadingMap().put(reading, new ArrayList<Long>());
		db.getReadingMap().get(reading).add(location);
		
		byte lastbyte = 0;
		if(split[7].equals("true"))//double_blind
			lastbyte = (byte) (lastbyte | 0x08);
		float dbl_blndHash = split[7].toLowerCase().hashCode();
		if(!db.getDoubleBlindMap().containsKey(dbl_blndHash))
			db.getDoubleBlindMap().put(dbl_blndHash, new ArrayList<Long>());
		db.getDoubleBlindMap().get(dbl_blndHash).add(location);
			
		if(split[8].equals("true"))//controlled_study
			lastbyte = (byte) (lastbyte | 0x04);
		float controlled_studyHash = split[8].toLowerCase().hashCode();
		if(!db.getControlledStudyMap().containsKey(controlled_studyHash))
			db.getControlledStudyMap().put(controlled_studyHash, new ArrayList<Long>());
		db.getControlledStudyMap().get(controlled_studyHash).add(location);
		
		if(split[9].equals("true"))//govt_funded
			lastbyte = (byte) (lastbyte | 0x02);
		float govt_fundedHash = split[9].toLowerCase().hashCode();
		if(!db.getGovt_fundedMap().containsKey(govt_fundedHash))
			db.getGovt_fundedMap().put(govt_fundedHash, new ArrayList<Long>());
		db.getGovt_fundedMap().get(govt_fundedHash).add(location);
		
		if(split[10].equals("true"))//fda_approved
			lastbyte = (byte) (lastbyte | 0x01);
		float fda_approvedHash = split[10].toLowerCase().hashCode();
		if(!db.getFda_approvedMap().containsKey(fda_approvedHash))
			db.getFda_approvedMap().put(fda_approvedHash, new ArrayList<Long>());
		db.getFda_approvedMap().get(fda_approvedHash).add(location);
		
		dos.writeByte(lastbyte);
		dos.flush();
	}

	private void writeIndexMapsToFiles(PharmaDatabase db) {
		String fn = db.getDbName();
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".id.ndx"))){
			oos.writeObject(db.getIdMap());
		} catch (IOException e) {
			System.err.println("Error while writing ID index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".company.ndx"))){
			oos.writeObject(db.getCompanyMap());
		} catch (IOException e) {
			System.err.println("Error while writing company index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".drug_id.ndx"))){
			oos.writeObject(db.getDrugIdMap());
		} catch (IOException e) {
			System.err.println("Error while writing drug_id index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".trials.ndx"))){
			oos.writeObject(db.getTrialsMap());
		} catch (IOException e) {
			System.err.println("Error while writing trials index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".patients.ndx"))){
			oos.writeObject(db.getPatientsMap());
		} catch (IOException e) {
			System.err.println("Error while writing patients index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".dosage_mg.ndx"))){
			oos.writeObject(db.getDosageMap());
		} catch (IOException e) {
			System.err.println("Error while writing dosage_mg index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".reading.ndx"))){
			oos.writeObject(db.getReadingMap());
		} catch (IOException e) {
			System.err.println("Error while writing reading index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".double_blind.ndx"))){
			oos.writeObject(db.getDoubleBlindMap());
		} catch (IOException e) {
			System.err.println("Error while writing double_blind index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".controlled_study.ndx"))){
			oos.writeObject(db.getControlledStudyMap());
		} catch (IOException e) {
			System.err.println("Error while writing controlled_study index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".govt_funded.ndx"))){
			oos.writeObject(db.getGovt_fundedMap());
		} catch (IOException e) {
			System.err.println("Error while writing govt_funded index file: "+e.getMessage());
		}
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fn+".fda_approved.ndx"))){
			oos.writeObject(db.getFda_approvedMap());
		} catch (IOException e) {
			System.err.println("Error while writing fda_approved index file: "+e.getMessage());
		}
	}

	private void loadExistingDatabases() {
		File[] files = findFilesEndingWith(BINARY_EXTENSION);
		if(files==null)
			return;
		for (File file : files) {
			PharmaDatabase db = new PharmaDatabase();
			String dbName = file.getName().substring(0, file.getName().indexOf(BINARY_EXTENSION));
			db.setDbName(dbName);
			loadCorrespondingIndexFiles(db);
			dbList.add(db);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadCorrespondingIndexFiles(PharmaDatabase db) {
		File idIndex = new File(db.getDbName()+".id.ndx");
		if(idIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(idIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>) {
					TreeMap<Float, ArrayList<Long>> idMap = (TreeMap<Float, ArrayList<Long>>)obj;
					db.setIdMap(idMap);
				}
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading ID index file: "+e.getMessage());
			} 
		}
		File companyIndex = new File(db.getDbName()+".company.ndx");
		if(companyIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(companyIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setCompanyMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading company index file: "+e.getMessage());
			} 
		}
		File drug_idIndex = new File(db.getDbName()+".drug_id.ndx");
		if(drug_idIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(drug_idIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setDrugIdMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading drug_id index file: "+e.getMessage());
			} 
		}
		File trialsIndex = new File(db.getDbName()+".trials.ndx");
		if(trialsIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(trialsIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setTrialsMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading trials index file: "+e.getMessage());
			} 
		}
		File patientsIndex = new File(db.getDbName()+".patients.ndx");
		if(patientsIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(patientsIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setPatientsMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading patients index file: "+e.getMessage());
			} 
		}
		File dosage_mgIndex = new File(db.getDbName()+".dosage_mg.ndx");
		if(dosage_mgIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dosage_mgIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setDosageMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading dosage_mg index file: "+e.getMessage());
			} 
		}
		File readingIndex = new File(db.getDbName()+".reading.ndx");
		if(readingIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(readingIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setReadingMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading reading index file: "+e.getMessage());
			} 
		}
		File double_blindIndex = new File(db.getDbName()+".double_blind.ndx");
		if(double_blindIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(double_blindIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setDoubleBlindMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading double_blind index file: "+e.getMessage());
			} 
		}
		File controlled_studyIndex = new File(db.getDbName()+".controlled_study.ndx");
		if(controlled_studyIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(controlled_studyIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setControlledStudyMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading controlled_study index file: "+e.getMessage());
			} 
		}
		File govt_fundedIndex = new File(db.getDbName()+".govt_funded.ndx");
		if(govt_fundedIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(govt_fundedIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setGovt_fundedMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading govt_funded index file: "+e.getMessage());
			} 
		}
		File fda_approvedIndex = new File(db.getDbName()+".fda_approved.ndx");
		if(fda_approvedIndex.exists()){
			try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fda_approvedIndex))) {
				Object obj = ois.readObject();
				if(obj instanceof TreeMap<?,?>)
					db.setFda_approvedMap((TreeMap<Float, ArrayList<Long>>)obj);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Error while reading fda_approved index file: "+e.getMessage());
			} 
		}
	}

	private File[] findFilesEndingWith(String extension){
    	File dir = new File(System.getProperty("user.dir"));

    	return dir.listFiles(new FilenameFilter() { 
    	         public boolean accept(File dir, String filename)
    	              { return filename.endsWith(extension); }
    	} );

    }

	private PharmaDatabase getDatabase(String dbName) {
		for (PharmaDatabase database : dbList) {
			if(database.getDbName().equalsIgnoreCase(dbName))
				return database;
		}
		return null;
	}

	private String formatLine(String line) {
		line = line.trim();
		if(line.toLowerCase().startsWith("insert")){
			int index = line.indexOf("(");
			String firstPart = line.substring(0, index).toLowerCase();
			String secondPart = line.substring(index);
			return firstPart+secondPart;
		}
		if(line.toLowerCase().startsWith("import")){
			String importPart = line.substring(0, 7).toLowerCase();
			String secondPart = line.substring(7);
			return importPart+secondPart;
		}
		return line.toLowerCase();
	}

}
