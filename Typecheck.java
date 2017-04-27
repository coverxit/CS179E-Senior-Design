import syntaxtree.*;
import visitor.*;

import java.lang.reflect.Method;
import java.util.*;

final class Helper {
    public static Identifier getClassName(Node c) {
        if (c instanceof MainClass) {
            /*
             * Grammar production:
             * f0 -> "class"
             * f1 -> Identifier()
             * f2 -> "{"
             * f3 -> "public"
             * f4 -> "static"
             * f5 -> "void"
             * f6 -> "main"
             * f7 -> "("
             * f8 -> "String"
             * f9 -> "["
             * f10 -> "]"
             * f11 -> Identifier()
             * f12 -> ")"
             * f13 -> "{"
             * f14 -> ( VarDeclaration() )*
             * f15 -> ( Statement() )*
             * f16 -> "}"
             * f17 -> "}"
             */
            return ((MainClass) c).f1;
        } else if (c instanceof ClassDeclaration) {
            /*
             * Grammar production:
             * f0 -> "class"
             * f1 -> Identifier()
             * f2 -> "{"
             * f3 -> ( VarDeclaration() )*
             * f4 -> ( MethodDeclaration() )*
             * f5 -> "}"
             */
            return ((ClassDeclaration) c).f1;
        } else if (c instanceof ClassExtendsDeclaration) {
            /*
             * Grammar production:
             * f0 -> "class"
             * f1 -> Identifier()
             * f2 -> "extends"
             * f3 -> Identifier()
             * f4 -> "{"
             * f5 -> ( VarDeclaration() )*
             * f6 -> ( MethodDeclaration() )*
             * f7 -> "}"
             */
            return ((ClassExtendsDeclaration) c).f1;
        } else {
            return null;
        }
    }

    public static Identifier getMethodName(Node c) {
        if (c instanceof MethodDeclaration) {
            /*
             * Grammar production:
             * f0 -> "public"
             * f1 -> Type()
             * f2 -> Identifier()
             * f3 -> "("
             * f4 -> ( FormalParameterList() )?
             * f5 -> ")"
             * f6 -> "{"
             * f7 -> ( VarDeclaration() )*
             * f8 -> ( Statement() )*
             * f9 -> "return"
             * f10 -> Expression()
             * f11 -> ";"
             * f12 -> "}"
             */
            return ((MethodDeclaration) c).;
        } else {
            return null;
        }
    }
}

class Symbol {
	private String name;
	private static Map<String, Symbol> dict = new HashMap<String, Symbol>();
	
	private Symbol(String n) {
		name = n;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static Symbol fromString(String n) {
		String u = n.intern();
		Symbol s = dict.get(u);
		
		if (s == null) {
			s = new Symbol(u);
			dict.put(u, s);
		}
		return s;
	}
}

class Binder {
	private Symbol symbol;
	private Node type;
    private Scope scope;
	
	public Binder(Symbol sy, Node t, Scope sc) {
		symbol = sy; type = t; scope = sc;
	}
	
	public Symbol getSymbol() {
		return symbol;
	}
	
	public Node getType() {
		return type;
	}

	public Scope getScope() { return scope; }
}

class Scope
{
	private Map<Symbol, Binder> table = new HashMap<Symbol, Binder>();
	
	private Scope parent;
	private List<Scope> children = new ArrayList<Scope>();
	
	public Scope() {
		parent = null;
	}
	
	public Scope(Scope p) {
		parent = p;
	}

	public void add(Symbol s, Node t) {
		table.put(s, new Binder(s, t, this));
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
	
	public Scope beginScope() {
		Scope s = new Scope(this);
		children.add(s);
		return s;
	}
	
	public Scope endScope() {
		return parent;
	}

	public boolean isRoot() {
		return parent == null;
	}
}

class ErrorMessage {
	private static boolean errors = false;
	
	public static void complain() {
		errors = true;
	}
	
	public static void complain(String msg) {
		errors = true;
		System.out.println(msg);
	}
	
	public static boolean anyErrors() {
		return errors;
	}
}

class FirstPhaseVisitor extends GJVoidDepthFirst<Scope> {
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	@Override
	public void visit(VarDeclaration n, Scope s) {
		n.f0.accept(this, s);
		String id = n.f1.f0.toString();
		
		
		
		n.f1.accept(this, s);
		n.f2.accept(this, s);
	}
}

class SecondPhaseVisitor extends GJDepthFirst<String, Symbol> {
	
}

public class Typecheck {
    public static void main(String[] args) throws ParseException { 
    	Scope root = new Scope();
    	
        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);

            Goal program = MiniJavaParser.Goal();
            program.accept(new FirstPhaseVisitor(), root);
            program.accept(new SecondPhaseVisitor(), null);
            
            if (!ErrorMessage.anyErrors())
            	System.out.println("Program type checked successfully");
            else
            	System.out.println("Type error");
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}