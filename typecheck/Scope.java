package typecheck;

import java.util.*;

import syntaxtree.*;

public class Scope {
    private Map<Symbol, Binder> table = new HashMap<>();
    private Scope parent;
    private Node bind;

    public Scope(Node b) {
        parent = null;
        bind = b;
    }

    public Scope(Scope p, Node b) {
        parent = p;
        bind = b;
    }

    public void add(Symbol sy, Node t, Scope sc) {
        table.put(sy, new Binder(sy, t, sc));
    }

    public Binder lookup(Symbol s) {
        Binder b = table.get(s);

        if (b != null)
            return b;
        // Lookup recursively
        else if (parent != null)
            return parent.lookup(s);
        else
            return null;
    }

    public Binder lookupLocal(Symbol s) {
        Binder b = table.get(s);

        if (b != null)
            return b;
        else
            return null;
    }

    public Scope getParent() {
        return parent;
    }

    public Node getNodeBound() {
        return bind;
    }

    public List<Symbol> getAllSymbols()
    {
	
	List<Symbol> SymbolList = new ArrayList<Symbol>();
	for(Map.Entry<Symbol, Binder> entry : table.entrySet())
	{
		Symbol key = entry.getKey();
		SymbolList.add(key);
	}
	if(parent != null)
	{
		SymbolList.addAll(parent.getAllSymbols());
	}
	
	return SymbolList;
    }
	
    public void printKeys()
    {
	for(Map.Entry<Symbol, Binder> entry : table.entrySet())
	{
		Symbol key = entry.getKey();
		Binder value = entry.getValue();
		String testVariable = "temp";
		try
		{
			testVariable = Helper.methodName(value.getType());
			System.out.println(key.toString() + " " + testVariable);
		}
		catch (ClassCastException e)
		{
			testVariable = "not method";
		}
	}
    }

    public void printScope()
    {
	for(Map.Entry<Symbol, Binder> entry : table.entrySet())
	{
		Symbol key = entry.getKey();
		Binder value = entry.getValue();
		System.out.print("CLASS " + key.toString() + "\n");
		value.getScope().printKeys();
	}
    }
}
