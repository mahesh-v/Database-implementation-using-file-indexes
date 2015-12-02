package data;

import java.util.ArrayList;
import java.util.TreeMap;

public class PharmaDatabase {
	private String dbName;
	private TreeMap<Float, ArrayList<Long>> idMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> companyMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> drugIdMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> trialsMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> patientsMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> dosageMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> readingMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> doubleBlindMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> controlledStudyMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> govt_fundedMap = new TreeMap<Float, ArrayList<Long>>();
	private TreeMap<Float, ArrayList<Long>> fda_approvedMap = new TreeMap<Float, ArrayList<Long>>();
	public String getDbName() {
		return dbName;
	}
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	public TreeMap<Float, ArrayList<Long>> getIdMap() {
		return idMap;
	}
	public void setIdMap(TreeMap<Float, ArrayList<Long>> indexMap) {
		this.idMap = indexMap;
	}
	public TreeMap<Float, ArrayList<Long>> getCompanyMap() {
		return companyMap;
	}
	public void setCompanyMap(TreeMap<Float, ArrayList<Long>> companyMap) {
		this.companyMap = companyMap;
	}
	public TreeMap<Float, ArrayList<Long>> getDrugIdMap() {
		return drugIdMap;
	}
	public void setDrugIdMap(TreeMap<Float, ArrayList<Long>> drugIdMap) {
		this.drugIdMap = drugIdMap;
	}
	public TreeMap<Float, ArrayList<Long>> getTrialsMap() {
		return trialsMap;
	}
	public void setTrialsMap(TreeMap<Float, ArrayList<Long>> trialsMap) {
		this.trialsMap = trialsMap;
	}
	public TreeMap<Float, ArrayList<Long>> getPatientsMap() {
		return patientsMap;
	}
	public void setPatientsMap(TreeMap<Float, ArrayList<Long>> patientsMap) {
		this.patientsMap = patientsMap;
	}
	public TreeMap<Float, ArrayList<Long>> getDosageMap() {
		return dosageMap;
	}
	public void setDosageMap(TreeMap<Float, ArrayList<Long>> dosageMap) {
		this.dosageMap = dosageMap;
	}
	public TreeMap<Float, ArrayList<Long>> getReadingMap() {
		return readingMap;
	}
	public void setReadingMap(TreeMap<Float, ArrayList<Long>> readingMap) {
		this.readingMap = readingMap;
	}
	public TreeMap<Float, ArrayList<Long>> getDoubleBlindMap() {
		return doubleBlindMap;
	}
	public void setDoubleBlindMap(TreeMap<Float, ArrayList<Long>> doubleBlindMap) {
		this.doubleBlindMap = doubleBlindMap;
	}
	public TreeMap<Float, ArrayList<Long>> getControlledStudyMap() {
		return controlledStudyMap;
	}
	public void setControlledStudyMap(TreeMap<Float, ArrayList<Long>> controlledStudyMap) {
		this.controlledStudyMap = controlledStudyMap;
	}
	public TreeMap<Float, ArrayList<Long>> getGovt_fundedMap() {
		return govt_fundedMap;
	}
	public void setGovt_fundedMap(TreeMap<Float, ArrayList<Long>> govt_fundedMap) {
		this.govt_fundedMap = govt_fundedMap;
	}
	public TreeMap<Float, ArrayList<Long>> getFda_approvedMap() {
		return fda_approvedMap;
	}
	public void setFda_approvedMap(TreeMap<Float, ArrayList<Long>> fda_approvedMap) {
		this.fda_approvedMap = fda_approvedMap;
	}
	
}
