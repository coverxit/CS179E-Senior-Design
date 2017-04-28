import java.util.*;

import syntaxtree.*;
import visitor.*;

/**
 * The second pass checks return value of methods, and checks statements, expressions and primary expressions.
 */
public class SecondPhaseVisitor extends GJDepthFirst<ExpressionType, Scope> {
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
