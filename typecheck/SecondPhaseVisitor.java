package typecheck;

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
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = s.lookup(id);

        n.f14.accept(this, b.getScope());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = s.lookup(id);

        n.f3.accept(this, b.getScope());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = s.lookup(id);

        n.f5.accept(this, b.getScope());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.methodName(n));
        Binder b = s.lookup(id);
        Scope ns = b.getScope();
        ExpressionType expect = TypeCheckHelper.makeExpressionType(n.f1);

        n.f4.accept(this, ns);
        n.f7.accept(this, ns);
        n.f8.accept(this, ns);

        if (TypeCheckHelper.isBasicType(expect.getType())
                || SubtypingRelation.contains(Symbol.fromString(expect.getType()))) {
            // Check return value type
            // A,C |- e:t
            ExpressionType actual = n.f10.accept(this, ns);
            if (!expect.equals(actual)) {
                if ((!TypeCheckHelper.isBasicType(actual.getType())) && (!TypeCheckHelper.isBasicType(expect.getType()))) {
                    if (SubtypingRelation.isSubtyping(Symbol.fromString(actual.getType()),
                            Symbol.fromString(expect.getType()))) {
                        return null;
                    }
                }

                if (!actual.getType().equals(TypeCheckHelper.UNDEFINED)
                        && !expect.getType().equals(TypeCheckHelper.UNDEFINED)) {
                    ErrorMessage.complain(n.f9.beginLine, "incompatible types: " + actual.getType() +
                            " cannot be converted to " + expect.getType());
                }
            }
        } else {
            ErrorMessage.complain(n.f0.beginLine, "cannot find symbol: class " + expect.getType());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f0));
        Binder b = s.lookupVar(id);

        // A(id) = t1
        if (b != null) {
            ExpressionType idt = TypeCheckHelper.makeExpressionType(TypeCheckHelper.extractTypeFromParamOrVar(b.getType()));
            ExpressionType et = n.f2.accept(this, s);

            // A,C |- e:t2
            if (!et.getType().equals(TypeCheckHelper.UNDEFINED)) {
                // t2 <= t1
                if (!idt.equals(et)) {
                    // if idtn and etn are both classes
                    if ((!TypeCheckHelper.isBasicType(idt.getType())) && (!TypeCheckHelper.isBasicType(et.getType()))) {
                        if (SubtypingRelation.isSubtyping(Symbol.fromString(et.getType()),
                                Symbol.fromString(idt.getType()))) {
                            return null;
                        }
                    }

                    if (!idt.getType().equals(TypeCheckHelper.UNDEFINED)) {
                        ErrorMessage.complain(n.f1.beginLine,
                                "incompatible types: " + et.getType() + " cannot be converted to " + idt.getType());
                    }
                }
            }
        } else {
            ErrorMessage.complain(n.f1.beginLine, "cannot find symbol: variable " + id.toString());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f0));
        Binder b = s.lookupVar(id);

        // A(id) = int[]
        if (b != null) {
            VarDeclaration vd = (VarDeclaration) b.getType();
            ExpressionType idt = TypeCheckHelper.makeExpressionType(vd.f0);

            if (idt.getType().equals(TypeCheckHelper.INT_ARRAY)) {
                ExpressionType indext = n.f2.accept(this, s);

                // A,C |- e1:int
                if (!indext.getType().equals(TypeCheckHelper.UNDEFINED)) {
                    if (indext.getType().equals(TypeCheckHelper.INT)) {
                        ExpressionType et = n.f5.accept(this, s);

                        // A,C |- e2:int
                        if (!et.getType().equals(TypeCheckHelper.UNDEFINED)) {
                            if (!et.getType().equals(TypeCheckHelper.INT)) {
                                ErrorMessage.complain(n.f4.beginLine,
                                        "incompatible types: " + et.getType() + " cannot be converted to int");
                            }
                        }
                    } else if (!indext.getType().equals(TypeCheckHelper.UNDEFINED)) {
                        ErrorMessage.complain(n.f1.beginLine,
                                "incompatible types: " + indext.getType() + " cannot be converted to int");
                    }
                }
            } else if (!idt.getType().equals(TypeCheckHelper.UNDEFINED)) {
                ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f0),
                        "array required, but " + idt.getType() + " found");
            }
        } else {
            ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f0),
                    "cannot find symbol: variable " + id.toString());
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
        if (condt.getType().equals(TypeCheckHelper.BOOLEAN)) {
            // A,C |- s1
            n.f4.accept(this, s);
            // A,C |- s2
            n.f6.accept(this, s);
        } else if (!condt.getType().equals(TypeCheckHelper.UNDEFINED)) {
            ErrorMessage.complain(n.f1.beginLine, "incompatible types: " + condt.getType() +
                    " cannot be converted to boolean");
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
        if (condt.getType().equals(TypeCheckHelper.BOOLEAN)) {
            // A,C |- s
            n.f4.accept(this, s);
        } else if (!condt.getType().equals(TypeCheckHelper.UNDEFINED)) {
            ErrorMessage.complain(n.f1.beginLine, "incompatible types: " + condt.getType() +
                    " cannot be converted to boolean");
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
        if (!expt.getType().equals(TypeCheckHelper.INT) && !expt.getType().equals(TypeCheckHelper.UNDEFINED)) {
            ErrorMessage.complain(n.f1.beginLine, "incompatible types: " + expt.getType() +
                    " cannot be converted to int");
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
        if (!lhs.getType().equals(TypeCheckHelper.BOOLEAN) || !rhs.getType().equals(TypeCheckHelper.BOOLEAN)) {
            ErrorMessage.complain(n.f1.beginLine, "bad operand types for binary operator '&&'");
        }

        // But anyway, we treat it as boolean
        // A,C |- p1 && p2:boolean
        return new ExpressionType(n, TypeCheckHelper.BOOLEAN);
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
        if (!lhs.getType().equals(TypeCheckHelper.INT) || !rhs.getType().equals(TypeCheckHelper.INT)) {
            ErrorMessage.complain(n.f1.beginLine, "bad operand types for binary operator '<'");
        }

        // But anyway, we treat it as boolean
        // A,C |- p1<p2:boolean
        return new ExpressionType(n, TypeCheckHelper.BOOLEAN);
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
        if (!lhs.getType().equals(TypeCheckHelper.INT) || !rhs.getType().equals(TypeCheckHelper.INT)) {
            ErrorMessage.complain(n.f1.beginLine, "bad operand types for binary operator '+'");
        }

        // But anyway, we treat it as int
        // A,C |- p1+p2:int
        return new ExpressionType(n, TypeCheckHelper.INT);
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
        if (!lhs.getType().equals(TypeCheckHelper.INT) || !rhs.getType().equals(TypeCheckHelper.INT)) {
            ErrorMessage.complain(n.f1.beginLine, "bad operand types for binary operator '-'");
        }

        // But anyway, we treat it as int
        // A,C |- p1-p2:int
        return new ExpressionType(n, TypeCheckHelper.INT);
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
        if (!lhs.getType().equals(TypeCheckHelper.INT) || !rhs.getType().equals(TypeCheckHelper.INT)) {
            ErrorMessage.complain(n.f1.beginLine, "bad operand types for binary operator '*'");
        }

        // But anyway, we treat it as int
        // A,C |- p1*p2:int
        return new ExpressionType(n, TypeCheckHelper.INT);
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
        if (array.getType().equals(TypeCheckHelper.INT_ARRAY)) {
            if (!index.getType().equals(TypeCheckHelper.INT) && !index.getType().equals(TypeCheckHelper.UNDEFINED)) {
                ErrorMessage.complain(n.f1.beginLine,
                        "incompatible types: " + index.getType() + " cannot be converted to int");
            }
        } else if (!array.getType().equals(TypeCheckHelper.UNDEFINED)) {
            ErrorMessage.complain(n.f1.beginLine, "array required, but " + array.getType() + " found");
        }

        // But anyway, we treat it as int
        // A,C |- p1[p2]:int
        return new ExpressionType(n, TypeCheckHelper.INT);
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
        if (!array.getType().equals(TypeCheckHelper.INT_ARRAY)) {
            if (!array.getType().equals(TypeCheckHelper.UNDEFINED)) {
                ErrorMessage.complain(n.f1.beginLine,
                        "cannot find symbol: variable length of type " + array.getType());
            }
        }

        // But anyway, we treat it as int
        // A,C |- p.length:int
        return new ExpressionType(n, TypeCheckHelper.INT);
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

        if (TypeCheckHelper.isBasicType(p.getType())) {
            type = TypeCheckHelper.UNDEFINED;
            ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f2),
                    "cannot find symbol: method " + TypeCheckHelper.identifierName(n.f2) + " of type " + p.getType());
        } else if (p.getType().equals(TypeCheckHelper.UNDEFINED)) {
            type = TypeCheckHelper.UNDEFINED;
            // ErrorMessage would be thrown by PrimaryExpression.
        } else { // p is Identifier
            // A,C |- p:D
            Binder b = s.lookup(Symbol.fromString(p.getType()));
            if (b != null) {
                Scope cs = b.getScope();

                // methodtype(D,id)=(t1',...,tn')->t
                Symbol m = Symbol.fromString(TypeCheckHelper.identifierName(n.f2));
                Binder mb = cs.lookup(m);

                if (mb != null) {
                    MethodType mt = TypeCheckHelper.methodType((MethodDeclaration) mb.getType());

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
                                // type mismatch?
                                if (!reft.equals(actt)) {
                                    if (!TypeCheckHelper.isBasicType(reft) && !TypeCheckHelper.isBasicType(actt)) {
                                        if (!SubtypingRelation.isSubtyping(Symbol.fromString(actt),
                                                Symbol.fromString(reft))) {
                                            ErrorMessage.complain(n.f3.beginLine,
                                                    "incompatible types: " + actt + " cannot be converted to " + reft);
                                        }
                                    } else {
                                        ErrorMessage.complain(n.f3.beginLine,
                                                "incompatible types: " + actt + " cannot be converted to " + reft);
                                    }
                                }
                            }
                        } else {
                            ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f2), "method " + m.toString() +
                                    " cannot be applied to given types (actual and formal argument lists differ in length)");
                        }
                    }

                    // But anyway, we treat it as its supposed return type
                    // A,C |- p.id(e1,...,en):t
                    type = mt.getReturnType().getType();
                } else {
                    type = TypeCheckHelper.UNDEFINED;
                    ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f2),
                            "cannot find symbol: method " + m.toString() +
                                    " in class " + TypeCheckHelper.className(cs.getNodeBound()));
                }
            } else {
                type = TypeCheckHelper.UNDEFINED;
                // ErrorMessage would be thrown by PrimaryExpression.
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
            type = TypeCheckHelper.INT;
        } else if (c.which == 1) { // TrueLiteral()
            // A,C |- true:boolean
            type = TypeCheckHelper.BOOLEAN;
        } else if (c.which == 2) { // FalseLiteral()
            // A,C |- false:boolean
            type = TypeCheckHelper.BOOLEAN;
        } else if (c.which == 3) { // Identifier()
            // id in dom(A) -> A,C |- id:A(id)
            Symbol id = Symbol.fromString(TypeCheckHelper.identifierName((Identifier) c.choice));
            Binder b = s.lookupVar(id);

            if (b != null) {
                return TypeCheckHelper.makeExpressionType(TypeCheckHelper.extractTypeFromParamOrVar(b.getType()));
            } else {
                type = TypeCheckHelper.UNDEFINED;
                ErrorMessage.complain(TypeCheckHelper.identifierLine((Identifier) c.choice),
                        "cannot find symbol: variable " + id.toString());
            }
        } else if (c.which == 4) { // ThisExpression()
            // A,C |- this:C
            type = TypeCheckHelper.className(s.getParent().getNodeBound());
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

            if (!et.getType().equals(TypeCheckHelper.INT) && !et.getType().equals(TypeCheckHelper.UNDEFINED)) {
                ErrorMessage.complain(((ArrayAllocationExpression) c.choice).f2.beginLine,
                        "incompatible types: " + et.getType() + " cannot be converted to int");
            }

            // But anyway we treat it as int[].
            type = TypeCheckHelper.INT_ARRAY;
        } else if (c.which == 6) { // AllocationExpression()
            // A,C |- new id():id
            /*
             * Grammar production:
             * f0 -> "new"
             * f1 -> Identifier()
             * f2 -> "("
             * f3 -> ")"
             */
            Identifier nt = ((AllocationExpression) c.choice).f1;
            Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(nt));

            if (SubtypingRelation.contains(id)) {
                type = id.toString();
            } else {
                type = TypeCheckHelper.UNDEFINED;
                ErrorMessage.complain(TypeCheckHelper.identifierLine(nt),
                        "cannot find symbol: class " + id.toString());
            }

        } else if (c.which == 7) { // NotExpression()
            // A,C |- e:boolean -> A,C |- !e:boolean
            /*
             * Grammar production:
             * f0 -> "!"
             * f1 -> Expression()
             */
            ExpressionType et = ((NotExpression) c.choice).f1.accept(this, s);

            if (!et.getType().equals(TypeCheckHelper.BOOLEAN) && !et.getType().equals(TypeCheckHelper.UNDEFINED)) {
                ErrorMessage.complain(((NotExpression) c.choice).f0.beginLine,
                        "bad operand type " + et.getType() + " for unary operator '!'");
            }

            // But anyway we treat it as boolean
            type = TypeCheckHelper.BOOLEAN;
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
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f1));
        ExpressionType rt = TypeCheckHelper.makeExpressionType(n.f0);

        if (!TypeCheckHelper.isBasicType(rt.getType())) {
            // Check if return type (class in this case) exist
            if (!SubtypingRelation.contains(Symbol.fromString(rt.getType()))) {
                ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f1),
                        "cannot find symbol: class " + rt.getType());
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
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f1));
        ExpressionType rt = TypeCheckHelper.makeExpressionType(n.f0);

        if (!TypeCheckHelper.isBasicType(rt.getType())) {
            // Check if return type (class in this case) exist
            if (!SubtypingRelation.contains(Symbol.fromString(rt.getType()))) {
                ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f1),
                        "cannot find symbol: class " + rt.getType());
            }
        }
        return null;
    }
}
