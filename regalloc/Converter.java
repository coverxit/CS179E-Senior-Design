package regalloc;

import codegen.Output;

import cs132.vapor.ast.*;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

public class Converter {
    private Output out;
    private RegisterPool localPool = RegisterPool.CreateLocalPool();

    public Converter(PrintStream s) {
        out = new Output(s);
    }

    public Output getOutput() {
        return out;
    }

    public void outputConstSegment(VDataSegment[] segments) {
        // Treat all data segment as const segment
        for (VDataSegment seg : segments) {
            out.writeLine("const " + seg.ident);
            out.increaseIndent();
            for (VOperand.Static label : seg.values) {
                out.writeLine(label.toString());
            }
            out.decreaseIndent();
            out.writeLine();
        }
    }

    public void outputAssignment(String lhs, String rhs) {
        out.writeLine(lhs + " = " + rhs);
    }

    private void outputFunctionSignature(String func, int inStack, int outStack, int localStack) {
        out.write("func " + func + " ");
        out.write("[in " + Integer.toString(inStack) + ", ");
        out.write("out " + Integer.toString(outStack) + ", ");
        out.writeLine("local " + Integer.toString(localStack) + "]");
    }

    private Register loadVariable(AllocationMap map, String var, boolean dst) {
        Register reg = map.lookupRegister(var);
        if (reg != null) { // var in register
            return reg;
        } else { // var on `local` stack
            int offset = map.lookupStack(var);
            Register load = localPool.acquire();
            if (!dst) // for dest's, they only want a register.
                outputAssignment(load.toString(), RegAllocHelper.local(offset));
            return load;
        }
    }

    private void writeVariable(Register reg, AllocationMap map, String var) {
        int offset = map.lookupStack(var);
        if (offset != -1) {
            outputAssignment(RegAllocHelper.local(offset), reg.toString());
        }
    }

    private void releaseLocalRegister(Register reg) {
        if (localPool.contains(reg))
            localPool.release(reg);
    }

    public void outputFunction(VFunction func, AllocationMap map, Liveness liveness) {
        List<Register> callee = map.usedCalleeRegister();

        // Map instrIndex to a label
        Map<Integer, Set<String>> labels = new HashMap<>();
        for (VCodeLabel l : func.labels)
            labels.computeIfAbsent(l.instrIndex, k -> new LinkedHashSet<>()).add(l.ident);

        int inStack = Math.max(func.params.length - 4, 0);
        int outStack = 0; // calculated later
        int localStack = map.stackSize();

        for (int i = 0; i < func.body.length; i++) {
            VInstr instr = func.body[i];
            if (instr instanceof VCall) {
                VCall call = (VCall) instr;
                outStack = Math.max(call.args.length - 4, outStack);

                // Only save those live-out but not def in this node.
                Set<String> liveOut = liveness.getOut().get(i);
                liveOut.removeAll(liveness.getDef().get(i));
                // For saving $t before function call.
                // $t are saved on the high address of local stack.
                int saves = (int) liveOut.stream().map(map::lookupRegister).filter(o -> o != null
                        && o.isCallerSaved()).distinct().count();
                localStack = Math.max(localStack, map.stackSize() + saves);
            }
        }

        outputFunctionSignature(func.ident, inStack, outStack, localStack);
        out.increaseIndent();

        // Save all $s registers
        for (int i = 0; i < callee.size(); i++) {
            outputAssignment(RegAllocHelper.local(i), callee.get(i).toString());
        }

        // Load parameters into register or `local` statck
        Register[] argregs = { Register.a0, Register.a1, Register.a2, Register.a3 };
        for (int i = 0; i < func.params.length; i++) {
            Register dst = map.lookupRegister(func.params[i].ident);
            if (dst != null) {
                if (i < 4) { // Params passed by registers
                    outputAssignment(dst.toString(), argregs[i].toString());
                } else { // Params passed by `in` stack
                    outputAssignment(dst.toString(), RegAllocHelper.in(i - 4));
                }
            } else {
                int offset = map.lookupStack(func.params[i].ident);
                if (offset != -1) { // some parameters may never be used
                    // Move the remaining parameters into `local` stack
                    Register load = localPool.acquire();
                    outputAssignment(load.toString(), RegAllocHelper.in(i - 4));
                    outputAssignment(RegAllocHelper.local(offset), load.toString());
                    localPool.release(load);
                }
            }
        }

        for (int i = 0; i < func.body.length; i++) {
            // Only save those live-out but not def in this node.
            final Set<String> liveOut = liveness.getOut().get(i);
            liveOut.removeAll(liveness.getDef().get(i));

            // Output labels
            if (labels.containsKey(i)) {
                out.decreaseIndent();
                labels.get(i).forEach(l -> out.writeLine(l + ":"));
                out.increaseIndent();
            }

            func.body[i].accept(new VInstr.Visitor<RuntimeException>() {
                @Override
                public void visit(VAssign vAssign) {
                    Register dst = loadVariable(map, vAssign.dest.toString(), true);

                    if (vAssign.source instanceof VVarRef) {
                        Register src = loadVariable(map, vAssign.source.toString(), false);
                        outputAssignment(dst.toString(), src.toString());
                        releaseLocalRegister(src);
                    } else {
                        outputAssignment(dst.toString(), vAssign.source.toString());
                    }

                    writeVariable(dst, map, vAssign.dest.toString());
                    releaseLocalRegister(dst);
                }

                @Override
                public void visit(VCall vCall) {
                    List<Register> save = liveOut.stream().map(map::lookupRegister).filter(o -> o != null
                            && o.isCallerSaved()).distinct().collect(Collectors.toList());
                    save.sort(Comparator.comparing(Register::toString));

                    // Save all $t registers
                    for (int i = 0; i < save.size(); i++) {
                        outputAssignment(RegAllocHelper.local(map.stackSize() + i), save.get(i).toString());
                    }

                    Register[] argregs = { Register.a0, Register.a1, Register.a2, Register.a3 };
                    for (int i = 0; i < vCall.args.length; i++) {
                        String var = vCall.args[i].toString();
                        if (vCall.args[i] instanceof VVarRef) {
                            if (i < 4) { // into registers
                                Register reg = map.lookupRegister(var);
                                if (reg != null) {
                                    outputAssignment(argregs[i].toString(), reg.toString());
                                } else {
                                    int offset = map.lookupStack(var);
                                    outputAssignment(argregs[i].toString(), RegAllocHelper.local(offset));
                                }
                            } else { // into `out` stack
                                Register reg = loadVariable(map, var, false);
                                outputAssignment(RegAllocHelper.out(i - 4), reg.toString());
                                releaseLocalRegister(reg);
                            }
                        } else {
                            if (i < 4) { // store into $a0~$a3
                                outputAssignment(argregs[i].toString(), var);
                            } else { // store into `out` stack
                                outputAssignment(RegAllocHelper.out(i - 4), var);
                            }
                        }
                    }

                    if (vCall.addr instanceof VAddr.Label) {
                        out.writeLine("call " + vCall.addr.toString());
                    } else {
                        Register addr = loadVariable(map, vCall.addr.toString(), false);
                        out.writeLine("call " + addr.toString());
                        releaseLocalRegister(addr);
                    }

                    Register dst = loadVariable(map, vCall.dest.toString(), true);
                    if (dst != Register.v0)
                        outputAssignment(dst.toString(), Register.v0.toString());
                    writeVariable(dst, map, vCall.dest.toString());
                    releaseLocalRegister(dst);

                    // Restore all $t registers
                    for (int i = 0; i < save.size(); i++) {
                        outputAssignment(save.get(i).toString(), RegAllocHelper.local(map.stackSize() + i));
                    }
                }

                @Override
                public void visit(VBuiltIn vBuiltIn) {
                    StringBuilder rhs = new StringBuilder(vBuiltIn.op.name + "(");
                    List<Register> srcregs = new ArrayList<>();
                    for (VOperand arg : vBuiltIn.args) {
                        if (arg instanceof VVarRef) {
                            Register src = loadVariable(map, arg.toString(), false);
                            srcregs.add(src);
                            rhs.append(src.toString()).append(" ");
                        } else {
                            rhs.append(arg.toString()).append(" ");
                        }
                    }
                    rhs.deleteCharAt(rhs.length() - 1).append(")");

                    for (Register src : srcregs)
                        releaseLocalRegister(src);

                    if (vBuiltIn.dest == null) { // no return value
                        out.writeLine(rhs.toString());
                    } else {
                        Register dst = loadVariable(map, vBuiltIn.dest.toString(), true);
                        outputAssignment(dst.toString(), rhs.toString());

                        writeVariable(dst, map, vBuiltIn.dest.toString());
                        releaseLocalRegister(dst);
                    }
                }

                @Override
                public void visit(VMemWrite vMemWrite) {
                    VMemRef.Global ref = (VMemRef.Global) vMemWrite.dest;
                    Register base = loadVariable(map, ref.base.toString(), false);

                    if (vMemWrite.source instanceof VVarRef) {
                        Register src = loadVariable(map, vMemWrite.source.toString(), false);
                        outputAssignment(RegAllocHelper.memoryReference(base, ref.byteOffset), src.toString());
                        releaseLocalRegister(src);
                    } else {
                        outputAssignment(RegAllocHelper.memoryReference(base, ref.byteOffset), vMemWrite.source.toString());
                    }

                    releaseLocalRegister(base);
                }

                @Override
                public void visit(VMemRead vMemRead) {
                    Register dst = loadVariable(map, vMemRead.dest.toString(), true);

                    VMemRef.Global ref = (VMemRef.Global) vMemRead.source;
                    Register src = loadVariable(map, ref.base.toString(), false);
                    outputAssignment(dst.toString(), RegAllocHelper.memoryReference(src, ref.byteOffset));
                    releaseLocalRegister(src);

                    writeVariable(dst, map, vMemRead.dest.toString());
                    releaseLocalRegister(dst);
                }

                @Override
                public void visit(VBranch vBranch) {
                    String cond = vBranch.value.toString();
                    if (vBranch.value instanceof VVarRef) {
                        Register src = loadVariable(map, vBranch.value.toString(), false);
                        cond = src.toString();
                        releaseLocalRegister(src);
                    }

                    out.write(vBranch.positive ? "if" : "if0");
                    out.write(" " + cond);
                    out.writeLine(" goto " + vBranch.target);
                }

                @Override
                public void visit(VGoto vGoto) {
                    out.writeLine("goto " + vGoto.target.toString());
                }

                @Override
                public void visit(VReturn vReturn) {
                    if (vReturn.value != null) {
                        if (vReturn.value instanceof VVarRef) {
                            Register src = loadVariable(map, vReturn.value.toString(), false);
                            if (src != Register.v0)
                                outputAssignment(Register.v0.toString(), src.toString());
                            releaseLocalRegister(src);
                        } else {
                            outputAssignment(Register.v0.toString(), vReturn.value.toString());
                        }
                    }

                    // Restore all $s registers
                    for (int i = 0; i < callee.size(); i++) {
                        outputAssignment(callee.get(i).toString(), RegAllocHelper.local(i));
                    }

                    out.writeLine("ret");
                }
            });
        }

        out.decreaseIndent();
    }
}
