package asmgen;

import codegen.Output;

import cs132.vapor.ast.*;
import regalloc.Register;

import java.util.*;

public class Assembler {
    private final Output out = new Output(System.out);
    private Set<VBuiltIn.Op> builtInUsed = new HashSet<>();
    private List<String> litStr = new ArrayList<>();

    public Output getOutput() {
        return out;
    }

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

    public void outputTextSegment() {
        out.writeLine(".text");
        out.writeLine();
        AsmGenHelper.jal("Main");
        AsmGenHelper.li(Register.v0, 10); // 10: exit
        AsmGenHelper.syscall();
        out.writeLine();
    }

    public void outputFunction(VFunction func) {
        out.writeLine(func.ident + ":");
        out.increaseIndent();

        // Map instrIndex to a label
        Map<Integer, Set<String>> labels = new HashMap<>();
        for (VCodeLabel l : func.labels)
            labels.computeIfAbsent(l.instrIndex, k -> new LinkedHashSet<>()).add(l.ident);

        // Adjust stack


        for (int i = 0; i < func.body.length; i++) {
            // Output labels
            if (labels.containsKey(i)) {
                out.decreaseIndent();
                labels.get(i).forEach(l -> out.writeLine(l + ":"));
                out.increaseIndent();
            }

            func.body[i].accept(new VInstr.Visitor<RuntimeException>() {
                @Override
                public void visit(VAssign vAssign) {
                    Register dst = Register.fromString(vAssign.dest.toString());
                    Register src = AsmGenHelper.loadOperand(vAssign.source, dst);

                    if (src != dst)
                        AsmGenHelper.move(dst, src);
                }

                @Override
                public void visit(VCall vCall) {
                    if (vCall.addr instanceof VAddr.Var)
                        AsmGenHelper.jalr(Register.fromString(vCall.addr.toString()));
                    else
                        AsmGenHelper.jal(((VAddr.Label) vCall.addr).label.ident);
                }

                @Override
                public void visit(VBuiltIn vBuiltIn) {
                    builtInUsed.add(vBuiltIn.op);

                    if (vBuiltIn.op == VBuiltIn.Op.Error) {
                        String msg = ((VLitStr) vBuiltIn.args[0]).value;
                        int index = litStr.indexOf(msg);
                        if (index == -1) {
                            index = litStr.size();
                            litStr.add(msg);
                        }

                        AsmGenHelper.la(Register.a0, "_str" + Integer.toString(index));
                        AsmGenHelper.j("_Error");
                    } else if (vBuiltIn.op == VBuiltIn.Op.PrintInt
                            || vBuiltIn.op == VBuiltIn.Op.PrintIntS) {
                        Register load = AsmGenHelper.loadOperand(vBuiltIn.args[0], Register.a0);
                        if (load != Register.a0)
                            AsmGenHelper.move(Register.a0, load);
                        AsmGenHelper.jal("_PrintIntS");
                    } else if (vBuiltIn.op == VBuiltIn.Op.HeapAlloc
                            || vBuiltIn.op == VBuiltIn.Op.HeapAllocZ) {
                        Register load = AsmGenHelper.loadOperand(vBuiltIn.args[0], Register.a0);
                        if (load != Register.a0)
                            AsmGenHelper.move(Register.a0, load);
                        AsmGenHelper.jal("_HeapAlloc");
                        AsmGenHelper.move(Register.fromString(vBuiltIn.dest.toString()), Register.v0);
                    } else {
                        Register dst = Register.fromString(vBuiltIn.dest.toString());
                        Register reg1 = AsmGenHelper.loadOperand(vBuiltIn.args[0]);
                        // src2 can be imm or reg
                        String src2 = vBuiltIn.args[1] instanceof VLitInt ?
                                Integer.toString(((VLitInt) vBuiltIn.args[1]).value) :
                                vBuiltIn.args[1].toString();

                        if (vBuiltIn.op == VBuiltIn.Op.Add) {
                            AsmGenHelper.addu(dst, reg1, src2);
                        } else if (vBuiltIn.op == VBuiltIn.Op.Sub) {
                            AsmGenHelper.subu(dst, reg1, src2);
                        } else if (vBuiltIn.op == VBuiltIn.Op.MulS) {
                            AsmGenHelper.mul(dst, reg1, src2);
                        } else if (vBuiltIn.op == VBuiltIn.Op.Eq) {
                            AsmGenHelper.seq(dst, reg1, src2);
                        } else if (vBuiltIn.op == VBuiltIn.Op.LtS) {
                            if (vBuiltIn.args[1] instanceof VLitInt)
                                AsmGenHelper.slti(dst, reg1, ((VLitInt) vBuiltIn.args[1]).value);
                            else
                                AsmGenHelper.slt(dst, reg1, Register.fromString(vBuiltIn.args[1].toString()));
                        } else if (vBuiltIn.op == VBuiltIn.Op.Lt) {
                            if (vBuiltIn.args[1] instanceof VLitInt)
                                AsmGenHelper.sltiu(dst, reg1, ((VLitInt) vBuiltIn.args[1]).value);
                            else
                                AsmGenHelper.sltu(dst, reg1, Register.fromString(vBuiltIn.args[1].toString()));
                        }
                    }
                }

                @Override
                public void visit(VMemWrite vMemWrite) {
                    AsmGenHelper.RegRef ref = AsmGenHelper.loadMemRef(vMemWrite.dest, func.stack);
                    AsmGenHelper.sw(AsmGenHelper.loadOperand(vMemWrite.source),
                            ref.getOffset(), ref.getRegister());
                }

                @Override
                public void visit(VMemRead vMemRead) {
                    AsmGenHelper.RegRef ref = AsmGenHelper.loadMemRef(vMemRead.source, func.stack);
                    AsmGenHelper.lw(Register.fromString(vMemRead.dest.toString()),
                            ref.getOffset(), ref.getRegister());
                }

                @Override
                public void visit(VBranch vBranch) {
                    if (vBranch.positive)
                        AsmGenHelper.bnez(AsmGenHelper.loadOperand(vBranch.value), vBranch.target.ident);
                    else
                        AsmGenHelper.beqz(AsmGenHelper.loadOperand(vBranch.value), vBranch.target.ident);
                }

                @Override
                public void visit(VGoto vGoto) {
                    if (vGoto.target instanceof VAddr.Var) { // goto $r
                        AsmGenHelper.jr(Register.fromString(vGoto.target.toString()));
                    } else { // goto :label
                        AsmGenHelper.j(((VAddr.Label) vGoto.target).label.ident);
                    }
                }

                @Override
                public void visit(VReturn vReturn) {
                    // Balance stack

                    AsmGenHelper.jr(Register.ra);
                }
            });
        }

        out.decreaseIndent();
        out.writeLine();
    }

    public void outputBuiltInFunctions() {
        if (builtInUsed.contains(VBuiltIn.Op.PrintInt)
                || builtInUsed.contains(VBuiltIn.Op.PrintIntS)) {
            out.writeLine("_PrintIntS:");
            out.increaseIndent();
            AsmGenHelper.li(Register.v0, 1); // 1: print integer
            AsmGenHelper.syscall();
            AsmGenHelper.la(Register.a0, "_lf");
            AsmGenHelper.syscall();
            AsmGenHelper.li(Register.v0, 4); // 4: pritn string
            AsmGenHelper.syscall();
            AsmGenHelper.jr(Register.ra);
            out.decreaseIndent();
            out.writeLine();
        }

        if (builtInUsed.contains(VBuiltIn.Op.Error)) {
            out.writeLine("_Error:");
            out.increaseIndent();
            AsmGenHelper.li(Register.v0, 4); // 4: print string
            AsmGenHelper.syscall();
            AsmGenHelper.li(Register.v0, 10); // 10: exit
            AsmGenHelper.syscall();
            out.decreaseIndent();
            out.writeLine();
        }

        if (builtInUsed.contains(VBuiltIn.Op.HeapAlloc)
                || builtInUsed.contains(VBuiltIn.Op.HeapAllocZ)) {
            out.writeLine("_HeapAlloc:");
            out.increaseIndent();
            AsmGenHelper.li(Register.v0, 9); // 9: sbrk
            AsmGenHelper.syscall();
            AsmGenHelper.jr(Register.ra);
            out.decreaseIndent();
            out.writeLine();
        }
    }

    public void outputConstSegment() {
        if (builtInUsed.contains(VBuiltIn.Op.PrintInt)
                || builtInUsed.contains(VBuiltIn.Op.PrintIntS)
                || builtInUsed.contains(VBuiltIn.Op.Error)) {
            out.writeLine(".data");
            out.writeLine(".align 0");

            if (builtInUsed.contains(VBuiltIn.Op.PrintInt) || builtInUsed.contains(VBuiltIn.Op.PrintIntS)) {
                out.writeLine(AsmGenHelper.literalString("_lf", "")); // literalString will append \n.
            }

            if (builtInUsed.contains(VBuiltIn.Op.Error)) {
                for (int i = 0; i < litStr.size(); i++) {
                    out.writeLine(AsmGenHelper.literalString("_str" + Integer.toString(i), litStr.get(i)));
                }
            }
            out.writeLine();
        }
    }
}
