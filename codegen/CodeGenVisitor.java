package codegen;

import java.util.*;

import syntaxtree.*;
import visitor.*;
import typecheck.*;

/**
 * For intermediate code generation.
 */
public class CodeGenVisitor extends GJDepthFirst<VariableLabel, CodeGenPair> {
    private boolean helperFunctionCalled = false;

    /*
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    @Override
    public VariableLabel visit(Goal n, CodeGenPair p) {
        Translator t = p.getTranslator();

        // Generate vtable
        for (Iterator<Binder> it = p.getScope().symbolIterator(); it.hasNext(); ) {
            Binder b = it.next();

            // We don't generate class record for MainClass
            if (!(b.getType() instanceof MainClass)) {
                t.generateClassRecord(b);
                t.outputVTable(b);
                t.getOutput().writeLine();
            }
        }

        n.f0.accept(this, p);
        n.f1.accept(this, p);

        if (helperFunctionCalled) {
            t.getOutput().writeLine();
            t.outputHelperFunction();
        }
        return null;
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
    public VariableLabel visit(MainClass n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = p.getScope().lookup(id);
        Scope ns = b.getScope();
        Translator t = p.getTranslator();

        t.outputMethodSignature(b);
        t.getOutput().increaseIndent();
        t.getLabelManager().beginMethod();

        n.f15.accept(this, new CodeGenPair(ns, t));
        t.outputReturn();

        t.getLabelManager().endMethod();
        t.getOutput().decreaseIndent();
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
    public VariableLabel visit(ClassDeclaration n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = p.getScope().lookup(id);

        n.f4.accept(this, new CodeGenPair(b.getScope(), p.getTranslator()));
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
    public VariableLabel visit(ClassExtendsDeclaration n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        Binder b = p.getScope().lookup(id);

        n.f6.accept(this, new CodeGenPair(b.getScope(), p.getTranslator()));
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
    public VariableLabel visit(MethodDeclaration n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.methodName(n));
        Binder b = p.getScope().lookup(id);
        Scope ns = b.getScope();
        Translator t = p.getTranslator();

        t.getOutput().writeLine();
        t.outputMethodSignature(b);
        t.getOutput().increaseIndent();
        t.getLabelManager().beginMethod();

        CodeGenPair np = new CodeGenPair(ns, t);
        n.f8.accept(this, np);
        VariableLabel ret = CodeGenHelper.retrieveDerefOrFuncCall(n.f10.accept(this, np), np);
        t.outputReturn(ret);

        t.getLabelManager().endMethod();
        t.getOutput().decreaseIndent();
        return null;
    }

    /*
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public VariableLabel visit(AssignmentStatement n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f0));
        Translator t = p.getTranslator();

        VariableLabel lhs = CodeGenHelper.identifierLabel(id, p);
        VariableLabel rhs = n.f2.accept(this, p);
        // "[this+n] = call t.i(...)" & "[this+n] = [this+m]" are not allowed.
        if (lhs.isDereference() && (rhs.isFunctionCall() || rhs.isDereference())) {
            rhs = CodeGenHelper.retrieveDerefOrFuncCall(rhs, p);
        }
        t.outputAssignment(lhs, rhs);
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
    public VariableLabel visit(ArrayAssignmentStatement n, CodeGenPair p) {
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f0));
        Translator t = p.getTranslator();
        VariableLabel idlbl = CodeGenHelper.identifierLabel(id, p);

        VariableLabel imm = CodeGenHelper.nullCheck(idlbl, p); // Null check first
        VariableLabel indlbl = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p); // Calculate index then
        VariableLabel elem = CodeGenHelper.boundsCheck(imm, indlbl, p); // Bounds check then
        VariableLabel rhs = n.f5.accept(this, p); // Calculate rhs finally

        // "[t.i+n] = call t.j(...)" & "[t.i+n] = [t.j+m]" are not allowed.
        if (rhs.isFunctionCall() || rhs.isDereference()) {
            rhs = CodeGenHelper.retrieveDerefOrFuncCall(rhs, p);
        }
        t.outputAssignment(elem, rhs);
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
    public VariableLabel visit(IfStatement n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();
        JumpLabel elselbl = lm.newIfElseJump();
        JumpLabel endlbl = lm.newIfEndJump();

        /*
            if0 t.0 goto :if0_else
                ...
                goto :if0_end
            if0_else:
                ...
            if0_end:
         */
        VariableLabel cond = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        t.outputIfNot(cond, elselbl);
        t.getOutput().increaseIndent();
        n.f4.accept(this, p);
        t.outputGoto(endlbl);
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(elselbl);
        t.getOutput().increaseIndent();
        n.f6.accept(this, p);
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(endlbl);
        return null;
    }

    /*
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public VariableLabel visit(WhileStatement n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();
        JumpLabel top = lm.newWhileTopJump();
        JumpLabel end = lm.newWhileEndJump();

        /*
            while0_top:
            if0 t.0 goto :while0_end
                ...
                goto :while0_top
            while0_end:
         */
        t.outputJumpLabel(top);

        VariableLabel cond = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        t.outputIfNot(cond, end);
        t.getOutput().increaseIndent();
        n.f4.accept(this, p);
        t.outputGoto(top);
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(end);
        return null;
    }

    /*
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public VariableLabel visit(PrintStatement n, CodeGenPair p) {
        Translator t = p.getTranslator();
        VariableLabel exp = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        t.outputPrintIntS(exp);
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
    public VariableLabel visit(Expression n, CodeGenPair p) {
        return n.f0.accept(this, p);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    public VariableLabel visit(AndExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();
        JumpLabel elselbl = lm.newAndElseJump();
        JumpLabel endlbl = lm.newAndEndJump();

        /*
            if0 t.1 goto :and0_else
                t.0 = t.2
                goto and0_end
            and0_else:
                t.0 = 0
            and0_end:
         */
        VariableLabel imm = lm.newTempVariable();
        VariableLabel cond1 = CodeGenHelper.retrieveDerefOrFuncCall(n.f0.accept(this, p), p);
        t.outputIfNot(cond1, elselbl);
        t.getOutput().increaseIndent();

        VariableLabel cond2 = n.f2.accept(this, p);
        t.outputAssignment(imm, cond2);
        t.outputGoto(endlbl);
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(elselbl);
        t.getOutput().increaseIndent();
        t.outputAssignment(imm, "0");
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(endlbl);
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public VariableLabel visit(CompareExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();

        VariableLabel lhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f0.accept(this, p), p);
        VariableLabel rhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        VariableLabel imm = lm.functionCall(CodeGenHelper.LtS(lhs.toString(), rhs.toString()));
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public VariableLabel visit(PlusExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();

        VariableLabel lhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f0.accept(this, p), p);
        VariableLabel rhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        VariableLabel imm = lm.functionCall(CodeGenHelper.Add(lhs.toString(), rhs.toString()));
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public VariableLabel visit(MinusExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();

        VariableLabel lhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f0.accept(this, p), p);
        VariableLabel rhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        VariableLabel imm = lm.functionCall(CodeGenHelper.Sub(lhs.toString(), rhs.toString()));
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public VariableLabel visit(TimesExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();

        VariableLabel lhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f0.accept(this, p), p);
        VariableLabel rhs = CodeGenHelper.retrieveDerefOrFuncCall(n.f2.accept(this, p), p);
        VariableLabel imm = lm.functionCall(CodeGenHelper.MulS(lhs.toString(), rhs.toString()));
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public VariableLabel visit(ArrayLookup n, CodeGenPair p) {
        VariableLabel array = n.f0.accept(this, p);
        VariableLabel imm = CodeGenHelper.nullCheck(array, p);
        VariableLabel indlbl = n.f2.accept(this, p);
        return CodeGenHelper.boundsCheck(imm, indlbl, p);
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public VariableLabel visit(ArrayLength n, CodeGenPair p) {
        Translator t = p.getTranslator();

        VariableLabel array = n.f0.accept(this, p);
        VariableLabel imm = CodeGenHelper.nullCheck(array, p);

        // t.0 = [t.0]
        t.outputAssignment(imm, imm.dereference());
        return imm;
    }

    /*
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public VariableLabel visit(MessageSend n, CodeGenPair p) {
        Translator t = p.getTranslator();
        ClassRecordManager cr = t.getClassRecordManager();
        LabelManager lm = t.getLabelManager();

        VariableLabel pvl = CodeGenHelper.nullCheck(n.f0.accept(this, p), p);

        // Use the second phase visitor from typechecker to get the expression type.
        ExpressionType pt = n.f0.accept(new SecondPhaseVisitor(), p.getScope());

        // pt should be Identifier()
        String cn = pt.getType();
        int offset = cr.lookupMethodOffset(cn, n.f2.f0.tokenImage);

        /*
            t.0 = [pvl]
            t.0 = [t.0+offset]
            receivee = call t.0(pvl params...)
         */
        VariableLabel imm = lm.newTempVariable();
        t.outputAssignment(imm, pvl.dereference());
        t.outputAssignment(imm, lm.localVariable(offset, imm.toString()).dereference());

        // parse parameter list
        ArrayList<VariableLabel> pl = new ArrayList<>();
        if (n.f4.present()) {
            ExpressionList el = (ExpressionList) n.f4.node;
            /*
             * f0 -> Expression()
             * f1 -> ( ExpressionRest() )*
             */
            pl.add(CodeGenHelper.retrieveDerefOrFuncCall(el.f0.accept(this, p), p));
            for (Enumeration<Node> e = el.f1.elements(); e.hasMoreElements(); ) {
                ExpressionRest er = (ExpressionRest) e.nextElement();

                /*
                 * f0 -> ","
                 * f1 -> Expression()
                 */
                pl.add(CodeGenHelper.retrieveDerefOrFuncCall(er.f1.accept(this, p), p));
            }
        }

        return lm.functionCall(CodeGenHelper.Call(imm, pvl, pl));
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
    public VariableLabel visit(PrimaryExpression n, CodeGenPair p) {
        Translator t = p.getTranslator();
        ClassRecordManager cr = t.getClassRecordManager();
        LabelManager lm = t.getLabelManager();
        VariableLabel imm;
        NodeChoice c = n.f0;

        if (c.which == 0) { // IntegerLiteral()
            /*
             * Grammar production:
             * f0 -> <INTEGER_LITERAL>
             */
            imm = lm.constant(((IntegerLiteral) c.choice).f0.tokenImage);
        } else if (c.which == 1) { // TrueLiteral()
            imm = lm.constant("1");
        } else if (c.which == 2) { // FalseLiteral()
            imm = lm.constant("0");
        } else if (c.which == 3) { // Identifier()
            Symbol id = Symbol.fromString(TypeCheckHelper.identifierName((Identifier) c.choice));
            imm = CodeGenHelper.identifierLabel(id, p);
        } else if (c.which == 4) { // ThisExpression()
            imm = lm.thisVariable();
        } else if (c.which == 5) { // ArrayAllocationExpression()
            /*
             * Grammar production:
             * f0 -> "new"
             * f1 -> "int"
             * f2 -> "["
             * f3 -> Expression()
             * f4 -> "]"
             */
            VariableLabel size = ((ArrayAllocationExpression) c.choice).f3.accept(this, p);

            /*
                receivee = call :AllocArray(size)
             */
            helperFunctionCalled = true;
            imm = lm.functionCall(CodeGenHelper.AllocArray(size));
        } else if (c.which == 6) { // AllocationExpression()
            /*
             * Grammar production:
             * f0 -> "new"
             * f1 -> Identifier()
             * f2 -> "("
             * f3 -> ")"
             */
            Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(((AllocationExpression) c.choice).f1));

            /*
                t.0 = HeapAllocZ(size)
                [t.0] = :vmt_class
             */
            imm = lm.newTempVariable();
            t.outputAssignment(imm, CodeGenHelper.HeapAllocZ(cr.sizeOfClass(id.toString())));
            t.outputAssignment(imm.dereference(), ":vmt_" + id.toString());
        } else if (c.which == 7) { // NotExpression()
            /*
             * Grammar production:
             * f0 -> "!"
             * f1 -> Expression()
             */
            VariableLabel rhs = ((NotExpression) c.choice).f1.accept(this, p);
            rhs = CodeGenHelper.retrieveDerefOrFuncCall(rhs, p);

            /*
                receivee = Sub(1 rhs)
             */
            imm = lm.functionCall(CodeGenHelper.Sub("1", rhs.toString()));
        } else { // c.which == 8, BracketExpression()
            /*
             * Grammar production:
             * f0 -> "("
             * f1 -> Expression()
             * f2 -> ")"
             */
            imm = ((BracketExpression) c.choice).f1.accept(this, p);
        }

        return imm;
    }

}