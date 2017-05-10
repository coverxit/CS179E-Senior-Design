package codegen;

import java.util.*;

import syntaxtree.*;
import visitor.*;
import typecheck.*;

/**
 * For intermediate code generation.
 */
public class CodeGenVisitor extends GJDepthFirst<VariableLabel, CodeGenPair> {
    private SecondPhaseVisitor spv = new SecondPhaseVisitor();

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

        t.outputHelperFunction();
        t.getOutput().writeLine();
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
        t.getOutput().writeLine();
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

        t.outputMethodSignature(b);
        t.getOutput().increaseIndent();
        t.getLabelManager().beginMethod();

        CodeGenPair np = new CodeGenPair(ns, t);
        n.f8.accept(this, np);
        t.outputReturn(n.f10.accept(this, np));

        t.getLabelManager().endMethod();
        t.getOutput().decreaseIndent();
        t.getOutput().writeLine();
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

        t.outputAssignment(CodeGenHelper.identifierLabel(id, p), n.f2.accept(this, p));
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
        VariableLabel indlbl = n.f2.accept(this, p); // Calculate index then
        VariableLabel elem = CodeGenHelper.boundsCheck(imm, indlbl, p); // Bounds check then
        VariableLabel rhs = n.f5.accept(this, p); // Calculate rhs finally

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
        t.outputIfNot(n.f2.accept(this, p), elselbl);
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
        t.getOutput().increaseIndent();

        t.outputIfNot(n.f2.accept(this, p), end);
        t.getOutput().increaseIndent();
        n.f4.accept(this, p);
        t.outputGoto(top);
        t.getOutput().decreaseIndent();

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
        p.getTranslator().outputPrintIntS(n.f2.accept(this, p));
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
        VariableLabel imm = lm.newTempVariable();
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
        t.outputIfNot(n.f0.accept(this, p), elselbl);
        t.getOutput().increaseIndent();
        t.outputAssignment(imm, n.f2.accept(this, p));
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
        VariableLabel imm = lm.newTempVariable();

        t.outputAssignment(imm, CodeGenHelper.LtS(n.f0.accept(this, p).toString(),
                n.f2.accept(this, p).toString()));

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
        VariableLabel imm = lm.newTempVariable();

        t.outputAssignment(imm, CodeGenHelper.Add(n.f0.accept(this, p).toString(),
                n.f2.accept(this, p).toString()));

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
        VariableLabel imm = lm.newTempVariable();

        t.outputAssignment(imm, CodeGenHelper.Sub(n.f0.accept(this, p).toString(),
                n.f2.accept(this, p).toString()));

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
        VariableLabel imm = lm.newTempVariable();

        t.outputAssignment(imm, CodeGenHelper.MulS(n.f0.accept(this, p).toString(),
                n.f2.accept(this, p).toString()));
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
        VariableLabel imm = lm.newTempVariable();

        // Use the second phase visitor from typechecker to get the expression type.
        ExpressionType pt = n.f0.accept(spv, p.getScope());
        VariableLabel pvl = CodeGenHelper.nullCheck(n.f0.accept(this, p), p);

        // pt should be Identifier()
        String cn = pt.getType();
        int offset = cr.lookupMethodOffset(cn, n.f2.f0.tokenImage);

        // parse parameter list
        ArrayList<VariableLabel> pl = new ArrayList<>();
        if (n.f4.present()) {
            ExpressionList el = (ExpressionList) n.f4.node;
            /*
             * f0 -> Expression()
             * f1 -> ( ExpressionRest() )*
             */
            pl.add(el.f0.accept(this, p));
            for (Enumeration<Node> e = el.f1.elements(); e.hasMoreElements(); ) {
                ExpressionRest er = (ExpressionRest) e.nextElement();

                /*
                 * f0 -> ","
                 * f1 -> Expression()
                 */
                pl.add(er.f1.accept(this, p));
            }
        }

        /*
            t.0 = [pvl]
            t.0 = [t.0+offset]
            t.0 = call t.0(pvl params...)
         */
        t.outputAssignment(imm, pvl.dereference());
        t.outputAssignment(imm, lm.localVariable(offset, imm.toString()).dereference());
        t.outputAssignment(imm, CodeGenHelper.Call(imm, pvl, pl));
        return imm;
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
            // A,C |- e:int -> A,C |- new int[e]:int[]
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
                t.0 = call :AllocArray(size)
             */
            imm = lm.newTempVariable();
            t.outputAssignment(imm, CodeGenHelper.AllocArray(size));
        } else if (c.which == 6) { // AllocationExpression()
            // A,C |- new id():id
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
            // A,C |- e:boolean -> A,C |- !e:boolean
            /*
             * Grammar production:
             * f0 -> "!"
             * f1 -> Expression()
             */
            VariableLabel rhs = ((NotExpression) c.choice).f1.accept(this, p);

            /*
                t.0 = Sub(1 rhs)
             */
            imm = lm.newTempVariable();
            t.outputAssignment(imm, CodeGenHelper.Sub("1", rhs.toString()));
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