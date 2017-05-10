package codegen;

import java.util.*;

import typecheck.*;

public class CodeGenHelper {
    public static String Add(String lhs, String rhs) {
        return "Add(" + lhs + " " + rhs + ")";
    }

    public static String Sub(String lhs, String rhs) {
        return "Sub(" + lhs + " " + rhs + ")";
    }

    public static String MulS(String lhs, String rhs) {
        return "MulS(" + lhs + " " + rhs + ")";
    }

    public static String Eq(String lhs, String rhs) {
        return "Eq(" + lhs + " " + rhs + ")";
    }

    public static String Lt(String lhs, String rhs) {
        return "Lt(" + lhs + " " + rhs + ")";
    }

    public static String LtS(String lhs, String rhs) {
        return "LtS(" + lhs + " " + rhs + ")";
    }

    public static String HeapAllocZ(int n) {
        return "HeapAllocZ(" + Integer.toString(n) + ")";
    }

    public static String AllocArray(VariableLabel size) {
        return "call :AllocArray(" + size.toString() + ")";
    }

    public static String Call(VariableLabel v, VariableLabel thislbl, ArrayList<VariableLabel> pl) {
        StringBuilder sb = new StringBuilder("call " + v.toString() + "(" + thislbl.toString());
        for (VariableLabel l : pl) {
            sb.append(" ");
            sb.append(l.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public static VariableLabel identifierLabel(Symbol id, CodeGenPair p)
    {
        VariableLabel label;
        Binder b = p.getScope().lookupLocal(id);
        Translator t = p.getTranslator();

        // if identifier found locally, no need to declare temp variable.
        if (b != null) {
            label = t.getLabelManager().localVariable(id.toString());
        } else {
            // p.getScope() is the scope of the method.
            // Thus its parent is the scope of the class.
            String cn = TypeCheckHelper.className(p.getScope().getParent().getNodeBound());
            int offset = t.getClassRecordManager().lookupVariableOffset(cn, id.toString());
            label = t.getLabelManager().thisVariable(offset).dereference();
        }
        return label;
    }

    public static VariableLabel retrieveDerefOrFuncCall(VariableLabel v, CodeGenPair p) {
        Translator t = p.getTranslator();
        VariableLabel imm = v;

        /*
            t.0 = v
            PrintIntS(t.0)
        */
        if (v.isDereference() || v.isFunctionCall()) {
            imm = t.getLabelManager().newTempVariable();
            t.outputAssignment(imm, v);
        }
        return imm;
    }

    public static VariableLabel nullCheck(VariableLabel l, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();
        VariableLabel var = l;

        if (!l.isThisPointer()) {
            JumpLabel jmp = lm.newNullJump();

            /*
                t.0 = l
                if t.0 goto :null0
                    Error("null pointer");
                null0:
             */
            if (l.isDereference() || l.isFunctionCall()) {
                var = lm.newTempVariable();
                t.outputAssignment(var, l);
            }

            t.outputIf(var, jmp);
            t.getOutput().increaseIndent();
            t.outputError("null pointer");
            t.getOutput().decreaseIndent();
            t.outputJumpLabel(jmp);
        }
        return var;
    }

    public static VariableLabel boundsCheck(VariableLabel l, VariableLabel ind, CodeGenPair p) {
        Translator t = p.getTranslator();
        LabelManager lm = t.getLabelManager();
        VariableLabel var = lm.newTempVariable();
        JumpLabel jmp = lm.newBoundsJump();

        /*
            t.0 = [l]
            t.0 = Lt(ind t.0)
            if t.0 goto :bounds0
                Error("array index out of bounds");
            bounds0:
            t.0 = MulS(ind 4)
            t.0 = Add(t.0 l)
         */
        l = retrieveDerefOrFuncCall(l, p);
        t.outputAssignment(var, l.dereference());
        t.outputAssignment(var, Lt(ind.toString(), var.toString()));

        t.outputIf(var, jmp);
        t.getOutput().increaseIndent();
        t.outputError("array index out of bounds");
        t.getOutput().decreaseIndent();

        t.outputJumpLabel(jmp);
        t.outputAssignment(var, MulS(ind.toString(), "4"));
        t.outputAssignment(var, Add(var.toString(), l.toString()));

        // Return "[t.0+4]"
        return lm.localVariable(4, var.toString()).dereference();
    }
}
