package asmgen;

import codegen.Output;

import cs132.vapor.ast.*;
import regalloc.Register;

import java.util.*;

public class Assembler {
    private final Output out = new Output(System.out);
    private Set<VBuiltIn.Op> builtInUsed = new HashSet<>();

    public void outputDataSegment(VDataSegment[] segments) {
        out.writeLine(".data");
        out.writeLine();

        for (VDataSegment seg : segments) {
            out.writeLine(seg.ident);
            out.increaseIndent();
            for (VOperand.Static label : seg.values) {
                out.writeLine(label.toString());
            }
            out.decreaseIndent();
            out.writeLine();
        }
    }

    private void outputBuiltInFunctions() {
        if (builtInUsed.contains(VBuiltIn.Op.PrintInt) || builtInUsed.contains(VBuiltIn.Op.PrintIntS)) {

        }

        if (builtInUsed.contains(VBuiltIn.Op.Error)) {

        }

        if (builtInUsed.contains(VBuiltIn.Op.HeapAlloc) || builtInUsed.contains(VBuiltIn.Op.HeapAllocZ)) {
            out.writeLine("_HeapAlloc:");
            out.increaseIndent();
            out.writeLine(AsmGenHelper.li(Register.v0, 9)); // 9: sbrk
            out.writeLine(AsmGenHelper.syscall());

            out.decreaseIndent();
        }
    }

    private void outputConstSegment() {

    }
}
