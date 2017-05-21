import java.util.*;

public class Record {

	private String className;
	private Record parentRecord;
	private Map<String, Integer> varMemTable = new HashMap<>();


	public Record(String name)
	{
		className = name;
	}
	public String getClassName()
	{
		return className;
	}
	public Record getParent()
	{
		return parentRecord;
	}
	public int getMemSize(String varLookUp)
	{
		int ret = 0;
		try
		{
			ret = varMemTable.get(varLookUp);
		}
		catch(NullPointerException e)
		{
			return 0;
		}
		return ret;
	}
	public void addVarMem(String var, int mem)
	{
		int recordsize = varMemTable.size() * 4;
		varMemTable.put(var, recordsize);
	}

	public int recordSize()
	{
		return varMemTable.size();
	}

	public void debugRecord()
	{
		System.out.println("RECORD OF CLASS: " + className);

		for(Map.Entry<String, Integer> entry : varMemTable.entrySet())
		{
			String key = entry.getKey();
			int value = entry.getValue();
			System.out.println("	" + key + " --- offset: " + value);
		}
	}

}
