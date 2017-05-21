import java.util.*;

public class PrintHelper {

	int indentlevel = 0;
	public PrintHelper()
	{
		indentlevel = 0;
	}
	public void increaseindent()
	{
		indentlevel = indentlevel+1;
	}
	public void decreaseindent()
	{
		if(indentlevel > 0)
		{
			indentlevel = indentlevel - 1;
		}
	}

	public void printline(String s)
	{
		String printstring = "";
		for(int i = 0; i < indentlevel; i++)
		{
			printstring = printstring + "	";
		}
		printstring = printstring + s;
		System.out.println(printstring);
	}
	public void printVtable(Vtable v)
	{
		String cn = v.getClassName();
		System.out.println("const " + "vmt_" + cn);
		//const vmt_class
		//	:class.method1
		//	:class.method2
		Map<String, Integer> vt = v.getTable();
		for(Map.Entry<String, Integer> entry : vt.entrySet())
		{
			String key = entry.getKey();
			System.out.println("	:" + cn + "." + key);
		}
		System.out.println();
	}


}
