import java.util.*;

import syntaxtree.*;
import visitor.*;

class HelperException extends Exception {
    public HelperException(String message) {
        super(message);
    }
}

class MethodType {
    private final ArrayList<String> parameters;
    private final String returnType;

    public MethodType(ArrayList<String> p, String rt) {
        parameters = new ArrayList<>(p);
        returnType = rt;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MethodType))
            return false;

        MethodType rhs = (MethodType) obj;
        return rhs.parameters.equals(this.parameters)
                && rhs.returnType.equals(this.returnType);
    }
}

class Helper {
    public static String className(Node c) throws HelperException {
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
            return ((MainClass) c).f1.f0.tokenImage;
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
            return ((ClassDeclaration) c).f1.f0.tokenImage;
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
            return ((ClassExtendsDeclaration) c).f1.f0.tokenImage;
        } else {
            throw new HelperException("Type mismatch");
        }
    }

    public static String methodName(Node m) throws HelperException {
        if (m instanceof MethodDeclaration) {
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
            return ((MethodDeclaration) m).f2.f0.tokenImage;
        } else {
            throw new HelperException("Type mismatch");
        }
    }

    public static boolean parameterDistinct(FormalParameterList fpl) {
        // Check if FormalParameterList() are pairwise distinct
        Set<String> declared = new HashSet<>();

        /*
         * Grammar production:
         * f0 -> FormalParameter()
         * f1 -> ( FormalParameterRest() )*
         */
        FormalParameter first = fpl.f0;
        NodeListInterface rest = fpl.f1;

        /*
         * Grammar production:
         * f0 -> Type()
         * f1 -> Identifier()
         */
        declared.add(first.f1.f0.tokenImage);
        for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
             * f0 -> ","
             * f1 -> FormalParameter()
             */
            String id = ((FormalParameterRest) e.nextElement()).f1.f1.f0.tokenImage;

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean variableDistinct(NodeListInterface vds) {
        // Checks if ( VarDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = vds.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             * f2 -> ";"
             */
            String id = ((VarDeclaration) e.nextElement()).f1.f0.tokenImage;

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean methodDistinct(NodeListInterface mds) {
        // Checks if ( MethodDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = mds.elements(); e.hasMoreElements(); ) {
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
            String id = ((MethodDeclaration) e.nextElement()).f2.f0.tokenImage;

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean classDistinct(NodeListInterface tds) throws HelperException {
        // Checks if ( TypeDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = tds.elements(); e.hasMoreElements(); ) {
            String id;
            Node n = e.nextElement();

            if (n instanceof MainClass) {
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
                id = ((MainClass) n).f1.f0.tokenImage;
            } else if (n instanceof TypeDeclaration) {
                /*
                 * Grammar production:
                 * f0 -> ClassDeclaration()
                 *       | ClassExtendsDeclaration()
                 */
                NodeChoice c = ((TypeDeclaration) n).f0;
                if (c.which == 0)
                    id = ((ClassDeclaration) c.choice).f1.f0.tokenImage;
                else // c.which == 1
                    id = ((ClassExtendsDeclaration) c.choice).f1.f0.tokenImage;
            } else {
                throw new HelperException("Type mismatch");
            }

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static MethodType methodType(MethodDeclaration m) {
        ArrayList<String> params = new ArrayList<>();
        String retType;

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
        if (m.f4.present()) {
            /*
             * Grammar production:
             * f0 -> FormalParameter()
             * f1 -> ( FormalParameterRest() )*
             */
            FormalParameterList fpl = (FormalParameterList) m.f4.node;
            FormalParameter first = fpl.f0;
            NodeListInterface rest = fpl.f1;

            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             */
            params.add(first.f1.f0.tokenImage);
            for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
                /*
                 * Grammar production:
                 * f0 -> ","
                 * f1 -> FormalParameter()
                 */
                String id = ((FormalParameterRest) e.nextElement()).f1.f1.f0.tokenImage;
                params.add(id);
            }
        }

        /*
         * Grammar production:
         * f0 -> ArrayType()
         *       | BooleanType()
         *       | IntegerType()
         *       | Identifier()
         */
        NodeChoice c = m.f1.f0;
        if (c.which == 0)
            retType = "int[]";
        else if (c.which == 1)
            retType = "boolean";
        else if (c.which == 2)
            retType = "int";
        else // c.which == 3
            retType = ((Identifier) c.choice).f0.tokenImage;

        return new MethodType(params, retType);
    }
}

class Symbol {
    private String name;
    private static Map<String, Symbol> dict = new HashMap<>();

    private Symbol(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Symbol fromString(String n) {
        String u = n.intern();
        return dict.putIfAbsent(u, new Symbol(u));
    }
}

class Binder {
    private Symbol symbol;
    private Node type;
    private Scope scope;

    public Binder(Symbol sy, Node t, Scope sc) {
        symbol = sy;
        type = t;
        scope = sc;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Node getType() {
        return type;
    }

    public Scope getScope() {
        return scope;
    }
}

class Scope {
    private Map<Symbol, Binder> table = new HashMap<>();
    private Scope parent;

    public Scope() {
        parent = null;
    }

    public Scope(Scope p) {
        parent = p;
    }

    public void add(Symbol s, Node t) {
        assert !table.containsKey(s) : "Symbol existed";
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

/**
 * The first pass builds the symbol table, checks overloading and all distinct properties.
 */
class FirstPhaseVisitor extends GJVoidDepthFirst<Scope> {
    /*
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    @Override
    public void visit(Goal n, Scope s) {
        // Build a list of all classes
        NodeList cl = new NodeList(n.f0);
        for (Enumeration<Node> e = n.f1.elements(); e.hasMoreElements(); ) {
            cl.addNode(e.nextElement());
        }

        try {
            // distinct(classname(mc),classname(d1),...,classname(dn))
            if (Helper.classDistinct(cl)) {
                n.f0.accept(this, s);
                n.f1.accept(this, s);
            } else {
                ErrorMessage.complain("Class identifiers are not pairwise distinct.");
            }
        } catch (HelperException e) {
            ErrorMessage.complain("HelperException: " + e.getMessage());
        }
    }

    /*
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
    @Override
    public void visit(MainClass n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        s.add(id, n);

        // Method `public static void main(String[] id)` is ignored for overloading check,
        // since we all know parser would complain if some other classes defined this method.
        Scope ns = new Scope(s);
        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f14)) {
            n.f14.accept(this, ns);
            // Skip `( Statement() )*`
        } else {
            ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
    }

    /*
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public void visit(ClassDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        s.add(id, n);

        Scope ns = new Scope(s);
        // distinct(id1,...,idf)
        if (Helper.variableDistinct(n.f3)) {
            n.f3.accept(this, ns);

            // distinct(methodname(m1),...methodname(mk))
            if (Helper.methodDistinct(n.f4))
                n.f4.accept(this, ns);
            else
                ErrorMessage.complain("Method identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
        } else {
            ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
    }

    /*
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public void visit(ClassExtendsDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        s.add(id, n);

        Scope ns = new Scope(s);
        // distinct(id1,...,idf)
        if (Helper.variableDistinct(n.f5)) {
            n.f5.accept(this, ns);

            // distinct(methodname(m1),...methodname(mk))
            if (Helper.methodDistinct(n.f6))
                n.f6.accept(this, ns);
            else
                ErrorMessage.complain("Method identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
        } else {
            ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
    }

    /*
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
    @Override
    public void visit(MethodDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f2.f0.tokenImage);
        Binder b = s.lookup(id);

        // Overloading check
        // noOverloading(id,idp,methodname(mi))
        if (b != null) {
            MethodType base = Helper.methodType((MethodDeclaration) b.getType());
            MethodType inherit = Helper.methodType(n);

            if (base.equals(inherit)) {
                ErrorMessage.complain("Overloading is now allowed. " +
                        "In class " + id.toString());
                return;
            }
        }
        s.add(id, n);

        Scope ns = new Scope(s);
        if (n.f4.present()) {
            // distinct(idf1,...,idfn)
            if (Helper.parameterDistinct((FormalParameterList) n.f4.node)) {
                n.f4.accept(this, ns);
            } else {
                ErrorMessage.complain("Parameter identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
                return;
            }
        }

        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f7)) {
            n.f7.accept(this, ns);
        } else {
            ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public void visit(VarDeclaration n, Scope s) {
        s.add(Symbol.fromString(n.f1.f0.tokenImage), n);
    }
}

class SecondPhaseVisitor extends GJDepthFirst<Node, Scope> {

}

public class Typecheck {
    public static void main(String[] args) throws ParseException {
        Scope env = new Scope();

        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);
            Goal program = MiniJavaParser.Goal();

            program.accept(new FirstPhaseVisitor(), env);
            if (ErrorMessage.anyErrors()) {
                System.out.println("Type error");
            } else {
                program.accept(new SecondPhaseVisitor(), env);
                if (ErrorMessage.anyErrors())
                    System.out.println("Type error");
                else
                    System.out.println("Program type checked successfully");
            }
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}