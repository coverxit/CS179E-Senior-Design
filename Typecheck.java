import syntaxtree.*;
import visitor.*;
import java.util.*;

class Symbol {
	private String name;
	private static Dictionary<String, Symbol> dict = new Hashtable<String, Symbol>();
	
	private Symbol(String n) {
		name = n;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static Symbol fromString(String n) {
		String u = n.intern();
		Symbol s = (Symbol) dict.get(u);
		
		if (s == null) {
			s = new Symbol(u);
			dict.put(u, s);
		}
		return s;
	}
}

class Binder {
	private Symbol symbol;
	private String type;
	
	public Binder(Symbol s, String t) {
		symbol = s; type = t;
	}
	
	public Symbol getSymbol() {
		return symbol;
	}
	
	public String getType() {
		return type;
	}
}

class Bucket {
	private Symbol key;
	private Binder binding;
	private Bucket next;
	
	public Bucket(Symbol k, Binder b, Bucket n) {
		key = k; binding = b; next = n;
	}
	
	public Symbol getKey() { 
		return key; 
	}
	
	public Binder getBinding() {
		return binding;
	}
	
	public Bucket getNext() {
		return next;
	}
}

class Environment
{
	private static Dictionary<Symbol, Bucket> table = new Hashtable<Symbol, Bucket>();
	private static Stack<Stack<Symbol>> scope = new Stack<Stack<Symbol>>();
	
	public static void push(Symbol s, Binder b) {
		table.put(s, new Bucket(s, b, table.get(s)));
		
		if (!scope.empty())
			scope.peek().push(s);
	}
	
	public static Binder lookup(Symbol s) {
		for (Bucket b = table.get(s); b != null; b = b.getNext())
			if (s.equals(b.getKey()))
				return b.getBinding();
	
		return null;
	}
	
	public static void pop(Symbol s) {
		Bucket b = table.get(s);
		
		if (b != null)
			table.put(s, b.getNext());
	}
	
	public static void beginScope() {
		scope.push(new Stack<Symbol>());
	}
	
	public static void endScope() {
		Stack<Symbol> stack = scope.pop();
		while (!stack.empty())
			Environment.pop(stack.pop());
	}
}

class ErrorMessage {
	private static boolean errors = false;
	
	public static void complain(String msg) {
		errors = true;
		System.out.println(msg);
	}
	
	public static boolean anyErrors() {
		return errors;
	}
}

class FirstPhaseVisitor extends GJVoidDepthFirst<Symbol> {
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	@Override
	public void visit(VarDeclaration n, Symbol sym) {
		n.f0.accept(this, null);
		String id = n.f1.f0.toString();
		
		
	}
}

class SecondPhaseVisitor extends GJDepthFirst<String, Symbol> {
	
}

public class Typecheck {
    public static void main(String[] args) throws ParseException { 
    	// Environment env = new Environment();
    	
        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);
            MiniJavaParser.Goal().accept(new FirstPhaseVisitor(), null);
            MiniJavaParser.Goal().accept(new SecondPhaseVisitor(), null);
            
            if (!ErrorMessage.anyErrors())
            	System.out.println("Program type checked successfully");
            else
            	System.out.println("Type error");
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}