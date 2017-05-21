import java.util.*;

public class Passer {

	//stuff to pass into the visitor
	private PrintHelper pHelper;
	private List<Vtable> vtList;
	private List<Record> rList;

	//stuff to save info between visitor levels, pretty messy 
	private Vtable currentClass = new Vtable("null"); 
	private Record currentRecord = new Record("null");
	private String currentParams = "";
	private String leftLiteral = "";
	private String oper = "";
	private String rightLiteral = "";
	private String holdover = "";
	private String result = "";
	private int tempstack = 0;
	private int nullstack = 0;
	private int ifstack = 0;
	private int ifendstack = 0;
	private int whilestack = 0;
	private int whileendstack = 0;
	private List<String> classStack = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	private List<Map<String,String>> symbolTableStack = new ArrayList<Map<String,String>>();


	public Passer(PrintHelper ph, List<Vtable> vtl, List<Record> rl)
	{
		pHelper = ph;
		vtList = vtl;
		rList = rl;
		symbolTableStack.add(new HashMap<String,String>());
	}
	public void setCurrentRecord(String cn)
	{
		currentRecord = new Record("null");
		for(Record r : rList)
		{
			if(r.getClassName() == cn)
			{
				currentRecord = r;
			}
		}
	}

	public String getRecordName(String vari)
	{
		String ret = "null";
		int mem = currentRecord.getMemSize(vari);
		if(mem == 0)
		{
			return ret;
		}
		ret = "[this+" + mem + "]";
		return ret;
	}
	public Map<String,String> peekSymbolTableStack()
	{
		return symbolTableStack.get(symbolTableStack.size()-1);
	}

	public String lookupSymbol(String s)
	{
		Map<String,String> temptable = peekSymbolTableStack();
		return temptable.get(s);
	}
	public void pushSymbolTable()
	{
		symbolTableStack.add(new HashMap<String,String>(peekSymbolTableStack()));
	}
	public void popSymbolTable()
	{
		symbolTableStack.remove(symbolTableStack.size()-1);
	}
	public void addSymbolTable(String k, String v)
	{
		Map<String,String> tempTable = peekSymbolTableStack();
		tempTable.put(k, v);
		popSymbolTable();
		symbolTableStack.add(tempTable);
	}

	public void pushClassStack(String classname)
	{
		classStack.add(classname);
	}

	public String peekClassStack()
	{
		return classStack.get(classStack.size()-1);
	}

	public String popClassStack()
	{
		String ret = classStack.get(classStack.size()-1);
		classStack.remove(classStack.size()-1);
		return ret;
	}
	
	public String getAllocAmount(String objname)
	{
		String ret = "0";
		for(Record r : rList)
		{
			if(r.getClassName() == objname)
			{
				int s = (r.recordSize() * 4) + 4; 
				ret = String.valueOf(s);
			}
		}
		return ret;
	}

	public void addArgument(String a)
	{
		arguments.add(a);
	}

	public String getArgumentString()
	{
		String ret = "";
		for(String s : arguments)
		{
			ret = ret + " " + s;
		}
		return ret;
	}

	public void clearArgument()
	{
		arguments.clear();
	}
	public String newIfLabel()
	{
		ifstack = ifstack + 1;
		String ret = "if" + String.valueOf(ifstack) + "_else";
		return ret;
	}

	public String newWhileLabel()
	{
		ifstack = ifstack + 1;
		String ret = "while" + String.valueOf(ifstack) + "_top";
		return ret;
	}

	public String newWhileEndLabel()
	{
		ifstack = ifstack + 1;
		String ret = "while" + String.valueOf(ifstack) + "_end";
		return ret;
	}

	public String newIfEndLabel()
	{
		ifendstack = ifendstack + 1;
		String ret = "if" + String.valueOf(ifendstack) + "_end";
		return ret;
	}


	public String newNullLabel()
	{
		nullstack = nullstack + 1;
		String ret = "null" + String.valueOf(nullstack);
		return ret;
	}

	public String newTempVariable()
	{
		String ret = String.valueOf(tempstack);
		tempstack = tempstack + 1;
		return ret;
	}

	public String popTempVariable()
	{
		tempstack = tempstack - 1;
		String ret = String.valueOf(tempstack);
		return ret;
	}
	
	public String peekTempVariable()
	{
		String ret = String.valueOf(tempstack-1);
		return ret;
	}
	
	public void clearTempStack()
	{
		tempstack = 0;
	}
	
	public void setHoldover(String ho)
	{
		holdover = ho;
	}

	public String getHoldover()
	{
		return holdover;
	}

	public void setResult(String re)
	{
		result = re;
	}

	public String getResult()
	{
		return result;
	}

	public void addParams(String param)
	{
		currentParams = currentParams + " " + param;
	}

	public String getParams()
	{
		return currentParams;
	}
	
	public void clearParams()
	{
		currentParams = "";
	}

	public void setLeftLiteral(String left)
	{
		leftLiteral = left;
	}

	public void setRightLiteral(String right)
	{
		rightLiteral = right; 
	}

	public String getLeftLiteral()
	{
		return leftLiteral;
	}

	public String getRightLiteral()
	{
		return rightLiteral; 
	}
	
	public void setOper(String op)
	{
		oper = op; 
	}

	public String getOper()
	{
		return oper;
	}

	public void clearExpression()
	{
		oper = "";
		leftLiteral = "";
		rightLiteral = "";
	}
	
	public String getCurrentClass()
	{
		return currentClass.getClassName();
	}

	public void setCurrentClass(String classname)
	{
		for(Vtable v : vtList)
		{
			if(v.getClassName() == classname)
			{
				currentClass = v;
			}
		}
	}
	
	public PrintHelper getPrint()
	{
		return pHelper;
	}
	public void upindent()
	{
		pHelper.increaseindent();
	}
	public void downindent()
	{
		pHelper.decreaseindent();
	}
	public List<Vtable> getVList()
	{
		return vtList;
	}

	public Vtable getVtable(String classname)
	{
		Vtable tempClass = new Vtable("null");
		for(Vtable v : vtList)
		{
			if(v.getClassName() == classname)
			{
				tempClass = v;
			}
		}
		return tempClass;
	}

	public Record getRecord(String classname)
	{
		Record tempClass = new Record("null");
		for(Record r : rList)
		{
			if(r.getClassName() == classname)
			{
				tempClass = r;
			}
		}
		return tempClass;
	}
	public List<Record> getRecord()
	{
		return rList;
	}

	public String getClassRep(String s)
	{
		Vtable tempvt = new Vtable("null");
		String ret = "null";
		String lookfor = s;
		boolean match = false;

		while(!match)
		{
			ret = lookupSymbol(lookfor);
			tempvt = getVtable(ret);
			if(tempvt.getClassName() == "null")
			{
				lookfor = ret;
			}
			else
			{
				match = true;
			}
		}
		return ret;
	}

}
