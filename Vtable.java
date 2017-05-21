import java.util.*;

public class Vtable {

	private String className;
	private Vtable parentVtable;
	private Map<String, Integer> methMemTable = new HashMap<>();


	public Vtable(String name)
	{
		className = name;
	}
	public String getClassName()
	{
		return className;
	}
	public Vtable getParent()
	{
		return parentVtable;
	}
	public int getMemSize(String methodLookUp)
	{
		int ret = 0;
		try
		{
			ret = methMemTable.get(methodLookUp);
		}
		catch(NullPointerException e)
		{
			return 0;
		}
		return ret;
	}
	public void addMethMem(String method, int mem)
	{
		int methsize = methMemTable.size() * 4;
		methMemTable.put(method, methsize);
	}

	public Map<String,Integer> getTable()
	{
		return methMemTable;
	}
	
	public void debugVtable()
	{
		System.out.println("VTABLE OF CLASS: " + className);

		for(Map.Entry<String, Integer> entry : methMemTable.entrySet())
		{
			String key = entry.getKey();
			int value = entry.getValue();
			System.out.println("	" + key + " --- offset: " + value);
		}
	}

}
