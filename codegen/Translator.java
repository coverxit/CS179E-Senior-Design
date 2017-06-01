package codegen;

import java.io.*;
import java.util.*;

import syntaxtree.*;
import typecheck.*;

public class Translator {
    private ClassRecordManager cr = new ClassRecordManager();
    private LabelManager label = new LabelManager();
    private Output out;

    public Translator() {
        out = new Output(System.out);
    }

    public Translator(PrintStream s) {
        out = new Output(s);
    }

    public ClassRecordManager getClassRecordManager() {
        return cr;
    }

    public LabelManager getLabelManager() {
        return label;
    }

    public Output getOutput() {
        return out;
    }

    public void generateClassRecord(Binder b) {
        Stack<Scope> ancestor = new Stack<>();
        for (Scope scan = b.getScope(); scan != null; scan = scan.getParent()) {
            ancestor.push(scan);
        }

        // Pop the top scope (the scope for all classes)
        // Now the top of the stack is the root base class of c.
        ancestor.pop();

        // Scan through ancestors
        Map<String, String> method = new LinkedHashMap<>();
        while (!ancestor.empty()) {
            Scope scan = ancestor.pop();
            String cn = TypeCheckHelper.className(scan.getNodeBound());

            for (Iterator<Binder> it = scan.symbolIterator(); it.hasNext(); ) {
                Binder bind = it.next();

                if (bind.getType() instanceof MethodDeclaration) {
                    String mn = bind.getSymbol().toString();
                    method.put(mn, cn + "." + mn);
                } else { // b.getType() instanceof VariableDeclaration
                    // Store variables
                    cr.putVariable(b.getSymbol().toString(), bind.getSymbol().toString());
                }
            }
        }

        // Store vtable
        cr.putMethod(b.getSymbol().toString(), method);
    }

    public void outputError(String error) {
        out.writeLine("Error(\"" + error + "\")");
    }

    public void outputVTable(Binder b) {
        // Generate codes
        out.writeLine("const vmt_" + b.getSymbol().toString());
        out.increaseIndent();
        for (Iterator<String> it = cr.methodIterator(b.getSymbol().toString()); it.hasNext(); ) {
            out.writeLine(":" + it.next());
        }
        out.decreaseIndent();
    }

    public void outputMethodSignature(Binder b) {
        Node m = b.getType();

        if (m instanceof MainClass) {
            out.writeLine("func Main()");
        } else { // n instanceof MethodDeclaration
            Scope s = b.getScope();
            String cn = TypeCheckHelper.className(s.getParent().getNodeBound());

            out.write("func ");
            out.write(cn + "." + TypeCheckHelper.methodName(m));
            out.write("(this");

            // Output parameter list
            for (Iterator<Binder> it = s.symbolIterator(); it.hasNext(); ) {
                Binder bind = it.next();

                if (bind.getType() instanceof FormalParameter) {
                    out.write(" " + bind.getSymbol().toString());
                }
            }

            out.writeLine(")");
        }
    }

    public void outputPrintIntS(VariableLabel l) {
        out.writeLine("PrintIntS(" + l.toString() + ")");
    }

    public void outputAssignment(VariableLabel lhs, VariableLabel rhs) {
        outputAssignment(lhs, rhs.toString());
    }

    public void outputAssignment(VariableLabel lhs, String rhs) {
        out.writeLine(lhs.toString() + " = " + rhs);
    }

    public void outputIf(VariableLabel condlbl, JumpLabel gotolbl) {
        out.writeLine("if " + condlbl.toString() + " goto :" + gotolbl.toString());
    }

    public void outputIfNot(VariableLabel condlbl, JumpLabel gotolbl) {
        out.writeLine("if0 " + condlbl.toString() + " goto :" + gotolbl.toString());
    }

    public void outputGoto(JumpLabel jmp) {
        out.writeLine("goto :" + jmp.toString());
    }

    public void outputJumpLabel(JumpLabel jmp) {
        out.writeLine(jmp.toString() + ":");
    }

    public void outputReturn() {
        out.writeLine("ret");
    }

    public void outputReturn(VariableLabel v) {
        out.writeLine("ret " + v.toString());
    }

    public void outputHelperFunction() {
        out.writeLine("func AllocArray(size)");
        out.increaseIndent();
        out.writeLine("bytes = MulS(size 4)");
        out.writeLine("bytes = Add(bytes 4)");
        out.writeLine("v = HeapAllocZ(bytes)");
        out.writeLine("[v] = size");
        out.writeLine("ret v");
        out.decreaseIndent();
    }
}
