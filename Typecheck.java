import java.util.*;

import syntaxtree.*;
import visitor.*;

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
        relation.get(base).add(child);

        // Subtyping is transitive
        for (Iterator<Symbol> it = relation.keySet().iterator(); it.hasNext(); ) {
            Symbol s = it.next();

            if (s.equals(base)) {
                Set<Symbol> children = relation.get(s);
                if (children.contains(base))
                    children.add(child);
            }
        }
    }

    public static boolean isSubtyping(Symbol child, Symbol base) {
        if (relation.containsKey(base))
            return relation.get(base).contains(child);
        else
            return false;
    }

    public static boolean contains(Symbol c) {
        return relation.containsKey(c);
    }
}

class Helper {
    public static String className(Node c) {
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
            return Helper.identifierName(((MainClass) c).f1);
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
            return Helper.identifierName(((ClassDeclaration) c).f1);
        } else { // c instanceof ClassExtendsDeclaration
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
            return Helper.identifierName(((ClassExtendsDeclaration) c).f1);
        }
    }

    public static String methodName(Node m) {
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
        return Helper.identifierName(((MethodDeclaration) m).f2);
    }

    public static String identifierName(Identifier id) {
        return id.f0.tokenImage;
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
        declared.add(Helper.identifierName(first.f1));
        for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
             * f0 -> ","
             * f1 -> FormalParameter()
             */
            String id = Helper.identifierName(((FormalParameterRest) e.nextElement()).f1.f1);

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
            String id = Helper.identifierName(((VarDeclaration) e.nextElement()).f1);

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
            String id = Helper.methodName(e.nextElement());

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean classDistinct(NodeListInterface tds) {
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
                id = Helper.className(n);
            } else { // n instanceof TypeDeclaration
                /*
                 * Grammar production:
                 * f0 -> ClassDeclaration()
                 *       | ClassExtendsDeclaration()
                 */
                id = Helper.className(((TypeDeclaration) n).f0.choice);
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
            params.add(Helper.makeExpressionType(first.f0).getType());
            for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
                /*
                 * Grammar production:
                 * f0 -> ","
                 * f1 -> FormalParameter()
                 */
                String type = Helper.makeExpressionType(((FormalParameterRest) e.nextElement()).f1.f0).getType();
                params.add(type);
            }
        }

        return new MethodType(params, Helper.makeExpressionType(m.f1));
    }

    public static Type extractTypeFromParamOrVar(Node n) {
        if (n instanceof FormalParameter) {
            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             */
            return ((FormalParameter) n).f0;
        } else { // n instanceof VarDeclaration
            /*
             * f0 -> Type()
             * f1 -> Identifier()
             * f2 -> ";"
             */
            return ((VarDeclaration) n).f0;
        }
    }

    public static final String UNDEFINED = "";
    public static final String INT_ARRAY = "int[]";
    public static final String BOOLEAN = "boolean";
    public static final String INT = "int";

    public static ExpressionType makeExpressionType(Type t) {
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
            retType = Helper.identifierName((Identifier) c.choice);

        return new ExpressionType(c.choice, retType);
    }

    public static boolean isBasicType(String type) {
        return type.equals(Helper.INT_ARRAY)
                || type.equals(Helper.BOOLEAN)
                || type.equals(Helper.INT);
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

    public Node getNodeBinded() {
        return bind;
    }
}

class ErrorMessage {
    private static boolean errors = false;

    public static void complain(String msg) {
        errors = true;
        // System.out.println(msg);
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

        // distinct(classname(mc),classname(d1),...,classname(dn))
        if (Helper.classDistinct(cl)) {
            n.f0.accept(this, s);
            n.f1.accept(this, s);
        } else {
            ErrorMessage.complain("Goal: Class identifiers are not pairwise distinct.");
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id); // Reflexive

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // Method `public static void main(String[] id)` is ignored for overloading check,
        // since we all know parser would complain if some other classes defined this method.
        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f14)) {
            n.f14.accept(this, ns);
            // Skip `( Statement() )*`
        } else {
            ErrorMessage.complain("MainClass: Variable identifiers are not pairwise distinct. " +
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id);

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // distinct(id1,...,idf)
        if (Helper.variableDistinct(n.f3)) {
            n.f3.accept(this, ns);

            // distinct(methodname(m1),...methodname(mk))
            if (Helper.methodDistinct(n.f4))
                n.f4.accept(this, ns);
            else
                ErrorMessage.complain("ClassDeclaration: Overloading is not allowed. " +
                        "In class " + id.toString());
        } else {
            ErrorMessage.complain("ClassDeclaration: Variable identifiers are not pairwise distinct. " +
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id);

        Symbol base = Symbol.fromString(Helper.identifierName(n.f3));
        Binder b = s.lookup(base);
        if (b != null) {
            SubtypingRelation.insert(base, id);

            Scope ns = new Scope(b.getScope(), n);
            s.add(id, n, ns);

            // distinct(id1,...,idf)
            if (Helper.variableDistinct(n.f5)) {
                n.f5.accept(this, ns);

                // distinct(methodname(m1),...methodname(mk))
                if (Helper.methodDistinct(n.f6))
                    n.f6.accept(this, ns);
                else
                    ErrorMessage.complain("ClassExtendsDeclaration: Overloading is not allowed. " +
                            "In class " + id.toString());
            } else {
                ErrorMessage.complain("ClassExtendsDeclaration: Variable identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
            }
        } else {
            ErrorMessage.complain("ClassExtendsDeclaration: Base class '" + base.toString() + "' is not defined. " +
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
        Symbol id = Symbol.fromString(Helper.methodName(n));
        Binder b = s.lookup(id);

        // Overloading check
        // noOverloading(id,idp,methodname(mi))
        if (b != null) {
            MethodType base = Helper.methodType((MethodDeclaration) b.getType());
            MethodType inherit = Helper.methodType(n);

            // Guarantee only overriding
            if (!base.equals(inherit)) {
                ErrorMessage.complain("MethodDeclaration: Overloading is not allowed. " +
                        "Method: " + id.toString());
            }
        }

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        if (n.f4.present()) {
            // distinct(idf1,...,idfn)
            if (Helper.parameterDistinct((FormalParameterList) n.f4.node)) {
                n.f4.accept(this, ns);
            } else {
                ErrorMessage.complain("MethodDeclaration: Parameter identifiers are not pairwise distinct. " +
                        "In method " + id.toString());
            }
        }

        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f7)) {
            n.f7.accept(this, ns);
        } else {
            ErrorMessage.complain("MethodDeclaration: Variable identifiers are not pairwise distinct. " +
                    "In method " + id.toString());
        }
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public void visit(FormalParameter n, Scope s) {
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));

        // Type check for VarDeclaration is in second phase.
        s.add(id, n, s);
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public void visit(VarDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));

        // Type check for VarDeclaration is in second phase.
        s.add(id, n, s);
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
        Symbol id = Symbol.fromString(Helper.className(n));
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
        Symbol id = Symbol.fromString(Helper.className(n));
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
        Symbol id = Symbol.fromString(Helper.className(n));
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
        Symbol id = Symbol.fromString(Helper.methodName(n));
        Binder b = s.lookup(id);
        Scope ns = b.getScope();

        n.f8.accept(this, ns);

        // Check return value type
        // A,C |- e:t
        ExpressionType retType = n.f10.accept(this, ns);
        if (!Helper.makeExpressionType(n.f1).equals(retType)) {
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
    // Rule 23
    @Override
    public ExpressionType visit(AssignmentStatement n, Scope s) {
        Symbol id = Symbol.fromString(Helper.identifierName(n.f0));
        Binder b = s.lookup(id);

        // A(id) = t1
        if (b != null) {
            ExpressionType idt = Helper.makeExpressionType(Helper.extractTypeFromParamOrVar(b.getType()));
            ExpressionType et = n.f2.accept(this, s);

            // A,C |- e:t2
            if (!et.getType().equals(Helper.UNDEFINED)) {
                // t2 <= t1
                if (!idt.equals(et)) {
                    // if idtn and etn are both classes
                    if ((idt.getNode() instanceof Identifier) && (et.getNode() instanceof Identifier)) {
                        if (SubtypingRelation.isSubtyping(Symbol.fromString(et.getType()),
                                Symbol.fromString(idt.getType()))) {

                        }
                    } else {
                        ErrorMessage.complain("Assignment: Type mismatch. " +
                                "LHS: " + idt.getType() + ", RHS: " + et.getType());
                    }
                }
            } else {
                ErrorMessage.complain("Assignment: RHS type is not defined.");
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
    // Rule 24
    @Override
    public ExpressionType visit(ArrayAssignmentStatement n, Scope s) {
        Symbol id = Symbol.fromString(Helper.identifierName(n.f0));
        Binder b = s.lookup(id);

        // A(id) = int[]
        if (b != null) {
            VarDeclaration vd = (VarDeclaration) b.getType();
            ExpressionType idt = Helper.makeExpressionType(vd.f0);

            if (idt.getType().equals(Helper.INT_ARRAY)) {
                ExpressionType indext = n.f2.accept(this, s);

                // A,C |- e1:int
                if (!indext.getType().equals(Helper.UNDEFINED)) {
                    if (indext.getType().equals(Helper.INT)) {
                        ExpressionType et = n.f5.accept(this, s);

                        // A,C |- e2:int
                        if (!et.getType().equals(Helper.UNDEFINED)) {
                            if (!et.getType().equals(Helper.INT)) {
                                ErrorMessage.complain("ArrayAssignment: RHS type mismatch. " +
                                        "RHS: " + et.getType());
                            }
                        } else {
                            ErrorMessage.complain("ArrayAssignment: RHS type is not defined.");
                        }
                    } else {
                        ErrorMessage.complain("ArrayAssignment: Index type mismatch. " +
                                "Type: " + indext.getType());
                    }
                } else {
                    ErrorMessage.complain("ArrayAssignment: Index type is not defined.");
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


    /*
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    // Rule 25
    public ExpressionType visit(IfStatement n, Scope s) {
        ExpressionType condt = n.f2.accept(this, s);

        // A,C |- e:boolean
        if (condt.getType().equals(Helper.BOOLEAN)) {
            // A,C |- s1
            n.f4.accept(this, s);
            // A,C |- s2
            n.f6.accept(this, s);
        } else {
            ErrorMessage.complain("IfStatement: Condition is not boolean type. " +
                    "Type: " + condt.getType());
        }
        return null;
    }

    /*
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    // Rule 26
    @Override
    public ExpressionType visit(WhileStatement n, Scope s) {
        ExpressionType condt = n.f2.accept(this, s);

        // A,C |- e:boolean
        if (condt.getType().equals(Helper.BOOLEAN)) {
            // A,C |- s
            n.f4.accept(this, s);
        } else {
            ErrorMessage.complain("WhileStatement: Condition is not boolean type. " +
                    "Type: " + condt.getType());
        }
        return null;
    }

    /*
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    // Rule 27
    @Override
    public ExpressionType visit(PrintStatement n, Scope s) {
        ExpressionType expt = n.f2.accept(this, s);

        // A,C |- e:int
        if (!expt.getType().equals(Helper.INT)) {
            ErrorMessage.complain("PrintStatement: Expression is not boolean type. " +
                    "Type: " + expt.getType());
        }
        return null;
    }

    /*
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    @Override
    public ExpressionType visit(Expression n, Scope s) {
        return n.f0.accept(this, s);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    // Rule 28
    @Override
    public ExpressionType visit(AndExpression n, Scope s) {
        ExpressionType lhs = n.f0.accept(this, s);
        ExpressionType rhs = n.f2.accept(this, s);

        // A,C |- p1:boolean; A,C |- p2:boolean
        if (!lhs.getType().equals(Helper.BOOLEAN) || !rhs.getType().equals(Helper.BOOLEAN)) {
            ErrorMessage.complain("AndExpression: LHS or RHS is not boolean type.");
        }

        // But anyway, we treat it as boolean
        // A,C |- p1 && p2:boolean
        return new ExpressionType(n, Helper.BOOLEAN);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    // Rule 29
    @Override
    public ExpressionType visit(CompareExpression n, Scope s) {
        ExpressionType lhs = n.f0.accept(this, s);
        ExpressionType rhs = n.f2.accept(this, s);

        // A,C |- p1:int; A,C |- p2:int
        if (!lhs.getType().equals(Helper.INT) || !rhs.getType().equals(Helper.INT)) {
            ErrorMessage.complain("CompareExpression: LHS or RHS is not int type.");
        }

        // But anyway, we treat it as boolean
        // A,C |- p1<p2:boolean
        return new ExpressionType(n, Helper.BOOLEAN);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    // Rule 30
    @Override
    public ExpressionType visit(PlusExpression n, Scope s) {
        ExpressionType lhs = n.f0.accept(this, s);
        ExpressionType rhs = n.f2.accept(this, s);

        // A,C |- p1:int; A,C |- p2:int
        if (!lhs.getType().equals(Helper.INT) || !rhs.getType().equals(Helper.INT)) {
            ErrorMessage.complain("PlusExpression: LHS or RHS is not int type.");
        }

        // But anyway, we treat it as int
        // A,C |- p1+p2:int
        return new ExpressionType(n, Helper.INT);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    // Rule 31
    @Override
    public ExpressionType visit(MinusExpression n, Scope s) {
        ExpressionType lhs = n.f0.accept(this, s);
        ExpressionType rhs = n.f2.accept(this, s);

        // A,C |- p1:int; A,C |- p2:int
        if (!lhs.getType().equals(Helper.INT) || !rhs.getType().equals(Helper.INT)) {
            ErrorMessage.complain("MinusExpression: LHS or RHS is not int type.");
        }

        // But anyway, we treat it as int
        // A,C |- p1-p2:int
        return new ExpressionType(n, Helper.INT);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    // Rule 32
    @Override
    public ExpressionType visit(TimesExpression n, Scope s) {
        ExpressionType lhs = n.f0.accept(this, s);
        ExpressionType rhs = n.f2.accept(this, s);

        // A,C |- p1:int; A,C |- p2:int
        if (!lhs.getType().equals(Helper.INT) || !rhs.getType().equals(Helper.INT)) {
            ErrorMessage.complain("TimesExpression: LHS or RHS is not int type.");
        }

        // But anyway, we treat it as int
        // A,C |- p1*p2:int
        return new ExpressionType(n, Helper.INT);
    }


    /*
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    // Rule 33
    @Override
    public ExpressionType visit(ArrayLookup n, Scope s) {
        ExpressionType array = n.f0.accept(this, s);
        ExpressionType index = n.f2.accept(this ,s);

        // A,C |- p1:int[]; A,C |- p2:int
        if (!array.getType().equals(Helper.INT_ARRAY) || !index.getType().equals(Helper.INT)) {
            ErrorMessage.complain("ArrayLookup: Array is not int[] type or Index is not int type.");
        }

        // But anyway, we treat it as int
        // A,C |- p1[p2]:int
        return new ExpressionType(n, Helper.INT);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    // Rule 34
    @Override
    public ExpressionType visit(ArrayLength n, Scope s) {
        ExpressionType array = n.f0.accept(this, s);

        // A,C |- p:int[]
        if (!array.getType().equals(Helper.INT_ARRAY)) {
            ErrorMessage.complain("ArrayLength: Array is not int[] type.");
        }

        // But anyway, we treat it as int
        // A,C |- p.length:int
        return new ExpressionType(n, Helper.INT);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    // Rule 35
    @Override
    public ExpressionType visit(MessageSend n, Scope s) {
        ExpressionType p = n.f0.accept(this, s);
        String type;

        if (Helper.isBasicType(p.getType())) {
            type = Helper.UNDEFINED;
            ErrorMessage.complain("MessageSend: Attempt to calling method on basic type.");
        } else if (p.getType().equals(Helper.UNDEFINED)) {
            type = Helper.UNDEFINED;
            ErrorMessage.complain("MessageSend: Attempt to calling method on a undefined identifier. " +
                    "Identifier: " + Helper.identifierName((Identifier) p.getNode()));
        } else { // p is Identifier
            // A,C |- p:D
            Binder b = s.lookup(Symbol.fromString(p.getType()));
            if (b != null) {
                Scope cs = b.getScope();

                // methodtype(D,id)=(t1',...,tn')->t
                Symbol m = Symbol.fromString(Helper.identifierName(n.f2));
                Binder mb = cs.lookup(m);

                if (mb != null) {
                    MethodType mt = Helper.methodType((MethodDeclaration) mb.getType());

                    // A,C |- ei:ti, ti <= ti'
                    if (n.f4.present()) {
                        ArrayList<String> refparam = mt.getParameters();
                        ArrayList<String> actparam = new ArrayList<>();
                        ExpressionList el = (ExpressionList) n.f4.node;

                        /*
                         * f0 -> Expression()
                         * f1 -> ( ExpressionRest() )*
                         */
                        actparam.add(el.f0.accept(this, s).getType());
                        for (Enumeration<Node> e = el.f1.elements(); e.hasMoreElements(); ) {
                            ExpressionRest er = (ExpressionRest) e.nextElement();

                            /*
                             * f0 -> ","
                             * f1 -> Expression()
                             */
                            actparam.add(er.f1.accept(this, s).getType());
                        }

                        if (refparam.size() == actparam.size()) {
                            for (int i = 0; i < refparam.size(); i++) {
                                String reft = refparam.get(i);
                                String actt = actparam.get(i);

                                // A,C |- ei:ti; ti <= ti'
                                if (!reft.equals(actt)) {
                                    if (!Helper.isBasicType(reft) && !Helper.isBasicType(actt)) {
                                        if (!SubtypingRelation.isSubtyping(Symbol.fromString(actt),
                                                Symbol.fromString(reft))) {
                                            ErrorMessage.complain("MessageSend: Parameters type mismatch (no inheritance) " +
                                                    "when calling method " + m.toString());
                                        }
                                    } else {
                                        ErrorMessage.complain("MessageSend: Parameters type mismatch (basic vs class) " +
                                                "when calling method " + m.toString());
                                    }
                                }
                            }
                        } else {
                            ErrorMessage.complain("MessageSend: Parameters count mismatch when " +
                                    "calling method " + m.toString());
                        }
                    }

                    // But anyway, we treat it as its supposed return type
                    // A,C |- p.id(e1,...,en):t
                    type = mt.getReturnType().getType();
                } else {
                    type = Helper.UNDEFINED;
                    ErrorMessage.complain("MessageSend: Method " + m.toString() + " is not defined " +
                            "in class " + Helper.className(cs.getNodeBinded()));
                }
            } else {
                type = Helper.UNDEFINED;
                ErrorMessage.complain("MessageSend: Class not found.");
            }
        }

        return new ExpressionType(n, type);
    }

    /*
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    // Rule 36~44
    @Override
    public ExpressionType visit(PrimaryExpression n, Scope s) {
        NodeChoice c = n.f0;
        String type;

        if (c.which == 0) { // IntegerLiteral()
            // A,C |- c:int
            type = Helper.INT;
        } else if (c.which == 1) { // TrueLiteral()
            // A,C |- true:boolean
            type = Helper.BOOLEAN;
        } else if (c.which == 2) { // FalseLiteral()
            // A,C |- false:boolean
            type = Helper.BOOLEAN;
        } else if (c.which == 3) { // Identifier()
            // id in dom(A) -> A,C |- id:A(id)
            Symbol id = Symbol.fromString(Helper.identifierName((Identifier) c.choice));
            Binder b = s.lookup(id);

            if (b != null) {
                // TODO: check whether VarDeclaration & FormalParameter is enough.
                return Helper.makeExpressionType(Helper.extractTypeFromParamOrVar(b.getType()));
            } else {
                type = Helper.UNDEFINED;
                ErrorMessage.complain("PrimaryExpression: Identifier is not defined. " +
                        "Identifier: " + id.toString());
            }
        } else if (c.which == 4) { // ThisExpression()
            // A,C |- this:C
            type = Helper.className(s.getParent().getNodeBinded());
        } else if (c.which == 5) { // ArrayAllocationExpression()
            // A,C |- e:int -> A,C |- new int[e]:int[]
            /*
             * Grammar production:
             * f0 -> "new"
             * f1 -> "int"
             * f2 -> "["
             * f3 -> Expression()
             * f4 -> "]"
             */
            ExpressionType et = ((ArrayAllocationExpression) c.choice).f3.accept(this, s);

            if (!et.getType().equals(Helper.INT)) {
                ErrorMessage.complain("ArrayAllocationExpression: Index type mismatch. " +
                        "Type: " + et.getType());
            }

            // But anyway we treat it as int[].
            type = Helper.INT_ARRAY;

        } else if (c.which == 6) { // AllocationExpression()
            // A,C |- new id():id
            /*
             * Grammar production:
             * f0 -> "new"
             * f1 -> Identifier()
             * f2 -> "("
             * f3 -> ")"
             */
            Symbol id = Symbol.fromString(Helper.identifierName(((AllocationExpression) c.choice).f1));

            if (SubtypingRelation.contains(id)) {
                type = id.toString();
            } else {
                type = Helper.UNDEFINED;
                ErrorMessage.complain("AllocationExpression: Identifier is not defined. " +
                        "Identifier: " + id.toString());
            }

        } else if (c.which == 7) { // NotExpression()
            // A,C |- e:boolean -> A,C |- !e:boolean
            /*
             * Grammar production:
             * f0 -> "!"
             * f1 -> Expression()
             */
            ExpressionType et = ((NotExpression) c.choice).f1.accept(this, s);

            if (!et.getType().equals(Helper.BOOLEAN)) {
                ErrorMessage.complain("NotExpression: Expression is not boolean type. " +
                        "Type: " + et.getType());
            }

            // But anyway we treat it as boolean
            type = Helper.BOOLEAN;
        } else { // c.which == 8, BracketExpression()
            // A,C |- e:t -> A,C |- (e):t
            /*
             * Grammar production:
             * f0 -> "("
             * f1 -> Expression()
             * f2 -> ")"
             */
            return ((BracketExpression) c.choice).f1.accept(this, s);
        }

        return new ExpressionType(c.choice, type);
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public ExpressionType visit(FormalParameter n, Scope s) {
        // Check if Type() is defined.
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));
        ExpressionType rt = Helper.makeExpressionType(n.f0);

        if (!Helper.isBasicType(rt.getType())) {
            // Check if return type (class in this case) exist
            if (!SubtypingRelation.contains(Symbol.fromString(rt.getType()))) {
                ErrorMessage.complain("FormalParameter: Type is not defined. " +
                        "Type: " + rt.getType() + ", Identifier: " + id.toString());
            }
        }
        return null;
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public ExpressionType visit(VarDeclaration n, Scope s) {
        // Check if Type() is defined.
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));
        ExpressionType rt = Helper.makeExpressionType(n.f0);

        if (!Helper.isBasicType(rt.getType())) {
            // Check if return type (class in this case) exist
            if (!SubtypingRelation.contains(Symbol.fromString(rt.getType()))) {
                ErrorMessage.complain("VarDeclaration: Type is not defined. " +
                        "Type: " + rt.getType() + ", Identifier: " + id.toString());
            }
        }
        return null;
    }
}

public class Typecheck {
    public static void main(String[] args) throws ParseException {
        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);
            Goal program = MiniJavaParser.Goal();
            Scope env = new Scope(program);

            program.accept(new FirstPhaseVisitor(), env);
            if (ErrorMessage.anyErrors()) {
                System.out.println("Type error");
                System.exit(1);
            } else {
                program.accept(new SecondPhaseVisitor(), env);
                if (ErrorMessage.anyErrors()) {
                    System.out.println("Type error");
                    System.exit(1);
                } else {
                    System.out.println("Program type checked successfully");
                }
            }
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}