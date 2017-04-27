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
    private final ExpressionType returnType;

    public MethodType(ArrayList<String> p, ExpressionType rt) {
        parameters = new ArrayList<>(p);
        returnType = rt;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public ExpressionType getReturnType() {
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

class ExpressionType {
    private final Node node;
    private final String type;

    public ExpressionType(Node n, String t) {
        node = n;
        type = t;
    }

    public Node getNode() {
        return node;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ExpressionType))
            return false;

        ExpressionType rhs = (ExpressionType) obj;
        return rhs.type.equals(this.type);
    }
}

class SubtypingRelation {
    private static Map<Symbol, Set<Symbol>> relation = new LinkedHashMap<>();

    public static void insert(Symbol base, Symbol child) {
        relation.putIfAbsent(base, new HashSet<>());

        // Subtyping is transitive
        for (Iterator<Symbol> it = relation.keySet().iterator(); it.hasNext(); ) {
            Set<Symbol> children = relation.get(it.next());
            // Since subtyping is reflexive, child can be inserted into base.
            if (children.contains(base))
                children.add(child);
        }
    }

    public static boolean isSubtyping(Symbol child, Symbol base) {
        if (relation.containsKey(base))
            return relation.get(base).contains(child);
        else
            return false;
    }
}

class Helper {
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

        return new MethodType(params, Helper.expressionType(m.f1));
    }

    public static final String UNDEFINED = "";
    public static final String INT_ARRAY = "int[]";
    public static final String BOOLEAN = "boolean";
    public static final String INT = "int";

    public static ExpressionType expressionType(Type t) {
        String retType;

        /*
         * Grammar production:
         * f0 -> ArrayType()
         *       | BooleanType()
         *       | IntegerType()
         *       | Identifier()
         */
        NodeChoice c = t.f0;
        if (c.which == 0)
            retType = Helper.INT_ARRAY;
        else if (c.which == 1)
            retType = Helper.BOOLEAN;
        else if (c.which == 2)
            retType = Helper.INT;
        else // c.which == 3
            retType = ((Identifier) c.choice).f0.tokenImage;

        return new ExpressionType(c.choice, retType);
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
        return dict.computeIfAbsent(n, k -> new Symbol(k));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Symbol))
            return false;

        Symbol rhs = (Symbol) obj;
        return rhs.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

class Binder {
    private Symbol symbol;
    private Node type;
    private Scope scope;

    public Binder(Symbol sy, Node t, Scope nsc) {
        symbol = sy;
        type = t;
        scope = nsc;
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

    public void add(Symbol sy, Node t, Scope sc) {
        if (table.containsKey(sy)) // Oh oh!
            throw new Error("Symbol existed");

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
        SubtypingRelation.insert(id, id); // Reflexive

        Scope ns = new Scope(s);
        s.add(id, n, ns);

        // Method `public static void main(String[] id)` is ignored for overloading check,
        // since we all know parser would complain if some other classes defined this method.
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
        SubtypingRelation.insert(id, id);

        Scope ns = new Scope(s);
        s.add(id, n, ns);

        // distinct(id1,...,idf)
        if (Helper.variableDistinct(n.f3)) {
            n.f3.accept(this, ns);

            // distinct(methodname(m1),...methodname(mk))
            if (Helper.methodDistinct(n.f4))
                n.f4.accept(this, ns);
            else
                ErrorMessage.complain("Overloading is not allowed. " +
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
        SubtypingRelation.insert(id, id);

        Symbol base = Symbol.fromString(n.f3.f0.tokenImage);
        Binder b = s.lookup(base);
        if (b != null) {
            SubtypingRelation.insert(base, id);

            Scope ns = new Scope(b.getScope());
            s.add(id, n, ns);

            // distinct(id1,...,idf)
            if (Helper.variableDistinct(n.f5)) {
                n.f5.accept(this, ns);

                // distinct(methodname(m1),...methodname(mk))
                if (Helper.methodDistinct(n.f6))
                    n.f6.accept(this, ns);
                else
                    ErrorMessage.complain("Overloading is not allowed. " +
                            "In class " + id.toString());
            } else {
                ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
            }
        } else {
            ErrorMessage.complain("Base class '" + base.toString() + "' is not defined. " +
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

            // Guarantee only overriding
            if (!base.equals(inherit)) {
                ErrorMessage.complain("Overloading is not allowed. " +
                        "Method: " + id.toString());
                return;
            }
        }

        Scope ns = new Scope(s);
        s.add(id, n, ns);

        if (n.f4.present()) {
            // distinct(idf1,...,idfn)
            if (Helper.parameterDistinct((FormalParameterList) n.f4.node)) {
                n.f4.accept(this, ns);
            } else {
                ErrorMessage.complain("Parameter identifiers are not pairwise distinct. " +
                        "In method " + id.toString());
                return;
            }
        }

        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f7)) {
            n.f7.accept(this, ns);
        } else {
            ErrorMessage.complain("Variable identifiers are not pairwise distinct. " +
                    "In method " + id.toString());
        }
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public void visit(VarDeclaration n, Scope s) {
        s.add(Symbol.fromString(n.f1.f0.tokenImage), n, s);
    }
}

/**
 * The second pass checks return value of methods, and checks statements, expressions and primary expressions.
 */

class SecondPhaseVisitor extends GJDepthFirst<ExpressionType, Scope> {
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
    public ExpressionType visit(MainClass n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        Binder b = s.lookup(id);

        n.f15.accept(this, b.getScope());
        return null;
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
    public ExpressionType visit(ClassDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        Binder b = s.lookup(id);

        n.f4.accept(this, b.getScope());
        return null;
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
    public ExpressionType visit(ClassExtendsDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f1.f0.tokenImage);
        Binder b = s.lookup(id);

        n.f6.accept(this, b.getScope());
        return null;
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
    public ExpressionType visit(MethodDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(n.f2.f0.tokenImage);
        Binder b = s.lookup(id);
        Scope ns = b.getScope();

        n.f8.accept(this, ns);

        // Check return value type
        // A,C |- e:t
        ExpressionType retType = n.f10.accept(this, ns);
        if (!Helper.expressionType(n.f1).equals(retType)) {
            ErrorMessage.complain("Return type mismatch. " +
                    "In method: " + id.toString());
        }
        return null;
    }

    /*
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public ExpressionType visit(AssignmentStatement n, Scope s) {
        Symbol id = Symbol.fromString(n.f0.f0.tokenImage);
        Binder b = s.lookup(id);

        // A(id) = t1
        if (b != null) {
            ExpressionType idt = Helper.expressionType((Type) b.getType());
            ExpressionType et = n.f2.accept(this, s);

            // A,C |- e:t2
            if (et != null) {
                // t2 <= t1
                if (!idt.equals(et)) {
                    // if idtn and etn are both classes
                    if ((idt.getNode() instanceof Identifier) && (et.getNode() instanceof Identifier)) {
                        if (SubtypingRelation.isSubtyping(Symbol.fromString(et.getType()),
                                Symbol.fromString(idt.getType()))) {

                        }
                    } else {
                        ErrorMessage.complain("Assignment type mismatch. " +
                                "LHS: " + idt.getType() + ", RHS: " + et.getType());
                    }
                }
            } else {
                ErrorMessage.complain("Assignment: RHS type undefined.");
            }
        } else {
            ErrorMessage.complain("Assignment: Identifier is not defined. " +
                    "Identifier: " + id.toString());
        }

        return null;
    }

    /*
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public Node visit(ArrayAssignmentStatement n, Scope s) {
        Symbol id = Symbol.fromString(n.f0.f0.tokenImage);
        Binder b = s.lookup(id);

        // A(id) = int[]
        if (b != null) {
            ExpressionType idt = Helper.expressionType((Type) b.getType());

            if (idt.getType().equals(Helper.INT_ARRAY)) {
                ExpressionType indext = n.f2.accept(this, s);

                // A,C |- e1:int
                if (indext != null) {
                    if (indext.getType().equals(Helper.INT)) {
                        ExpressionType et = n.f5.accept(this, s);

                        // A,C |- e2: int
                        if (et != null) {
                            if (!et.getType().equals(Helper.INT)) {
                                ErrorMessage.complain("ArrayAssignment: RHS type mismatch. " +
                                        "RHS: " + et.getType());
                            }
                        } else {
                            ErrorMessage.complain("ArrayAssignment: RHS type undefined.");
                        }
                    } else {
                        ErrorMessage.complain("ArrayAssignment: Index type mismatch. " +
                                "Type: " + indext.getType());
                    }
                } else {
                    ErrorMessage.complain("ArrayAssignment: Index type undefined.");
                }

            } else {
                ErrorMessage.complain("ArrayAssignment: Identifier (LHS) type mismatch. " +
                        "Type: " + idt.getType());
            }
        } else {
            ErrorMessage.complain("ArrayAssignment: Identifier is not defined. " +
                    "Identifier: " + id.toString());
        }

        return null;
    }
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