package data;

public class WhereClause {
	public String clauseVariable;
	public boolean equalTo;
	public boolean not;
	public boolean greaterThan;
	public boolean lesserThan;
	public String str_val;
	public float flt_val;
	public String valType;
	
	public WhereClause(String clause) {
		evaluateClause(clause);
	}

	private void evaluateClause(String cl) {
		this.clauseVariable = getClauseVariable(cl);
		if(cl.contains("="))
			this.equalTo=true;
		if(cl.contains("!")||cl.contains("not"))
			this.not = true;
		if(cl.contains(">"))
			this.greaterThan = true;
		if(cl.contains("<"))
			this.lesserThan = true;
		String clauseVal = getClauseValue(cl);
		clauseVal = clauseVal.trim();
		if(clauseVal.startsWith("\"")&& clauseVal.endsWith("\""))
			clauseVal = clauseVal.substring(1, clauseVal.length()-1);
		try{
			flt_val = Float.parseFloat(clauseVal);
		} catch(NumberFormatException nfe){
			str_val = clauseVal;
		}
	}
	
	private String getClauseValue(String clause) {
		char[] c = clause.trim().toCharArray();
		for (int i = c.length-1; i >=0; i--) {
			if(c[i] == '>'||c[i] == '<'||c[i] == '='||c[i] == '!')
				return clause.trim().substring(i+1, c.length);
		}
		return null;
	}

	private String getClauseVariable(String clause) {
		char[] c = clause.trim().toCharArray();
		for (int i = 0; i < c.length; i++) {
			if(c[i] == ' '||c[i] == '>'||c[i] == '<'||c[i] == '='||c[i] == '!')
				return clause.substring(0, i);
		}
		return null;
	}
}
