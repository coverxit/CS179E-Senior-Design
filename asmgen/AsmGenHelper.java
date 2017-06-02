package asmgen;

import codegen.Output;
import regalloc.Register;

import cs132.vapor.ast.*;

public class AsmGenHelper {
    private static Output out;

    public AsmGenHelper(Output o) {
        out = o;
    }

    public static String literalString(String name, String value) {
        return name + ": .asciiz \"" + value + "\\n\"";
    }

    public static Register loadOperand(VOperand op) {
        // Use $t9 as the temp loading register
        return loadOperand(op, Register.t9);
    }

    public static Register loadOperand(VOperand op, Register load) {
        Register ret;
        if (op instanceof VLitInt) {
            VLitInt lit = (VLitInt) op;
            if (lit.value == 0) {
                ret = Register.zero;
            } else {
                AsmGenHelper.li(load, lit.value);
                ret = load;
            }
        } else if (op instanceof VLabelRef) {
            AsmGenHelper.la(load, ((VLabelRef) op).ident);
            ret = load;
        } else { // op instanceof VVarRef
            ret = Register.fromString(op.toString());
        }
        return ret;
    }

    static class RegRef {
        private final Register reg;
        private final int offset;

        public RegRef(Register r, int o) {
            reg = r;
            offset = o;
        }

        public Register getRegister() {
            return reg;
        }

        public int getOffset() {
            return offset;
        }
    }

    public static RegRef loadMemRef(VMemRef ref, VFunction.Stack stack) {
        Register reg;
        int offset;

        /*
           Callee frame:

           Higher address
                    ----------------------
                    |
           $fp ->   | In
                    ----------------------
                    |
                    | Local
                    ----------------------
                    |
           $sp ->   | Out
                    ----------------------
                    | ra
                    ----------------------
                    | fp
                    ----------------------
           Lower address
        */

        if (ref instanceof VMemRef.Global) {
            VMemRef.Global global = (VMemRef.Global) ref;
            reg = Register.fromString(global.base.toString());
            offset = global.byteOffset;
        } else { // vMemWrite.dest instanceof VMemRef.Stack
            VMemRef.Stack local = (VMemRef.Stack) ref;
            if (local.region == VMemRef.Stack.Region.In) {
                reg = Register.fp;
                offset = local.index * 4; // 4-byte align
            } else if (local.region == VMemRef.Stack.Region.Out) {
                reg = Register.sp;
                offset = local.index * 4;
            } else { // local.region == VMemRef.Stack.Region.Local
                reg = Register.sp;
                offset = (local.index + stack.out) * 4;
            }
        }

        return new RegRef(reg, offset);
    }

    /* MIPS Instructions */
    // move
    public static void move(Register dst, Register src) {
        Op("move", dst, src);
    }

    // li (load immediate)
    public static void li(Register reg, int imm) {
        Op("li", reg, imm);
    }

    // la (load address)
    public static void la(Register reg, String imm) {
        Op("la", reg, imm);
    }

    // syscall (system call)
    public static void syscall() {
        out.writeLine("syscall");
    }

    // j (jump)
    public static void j(String target) {
        Op("j", target);
    }

    // jal (jump and link)
    public static void jal(String target) {
        Op("jal", target);
    }

    public static void jalr(Register reg) {
        Op("jalr", reg);
    }

    // jr (jump register)
    public static void jr(Register reg) {
        Op("jr", reg);
    }

    public static void lw(Register dst, int offset, Register src) {
        Op("lw", dst, offset, src);
    }

    // sw (store word)
    public static void sw(Register src, int offset, Register dst) {
        Op("sw", src, offset, dst);
    }

    // addu (add unsigned)
    public static void addu(Register dst, Register reg1, String src2) {
        Op("addu", dst, reg1, src2);
    }

    // subu (subtract unsigned)
    public static void subu(Register dst, Register reg1, String src2) {
        Op("subu", dst, reg1, src2);
    }

    // mul (multiply)
    public static void mul(Register dst, Register reg1, String src2) {
        Op("mul", dst, reg1, src2);
    }

    // seq (set on equal)
    public static void seq(Register dst, Register reg1, String src2) {
        Op("seq", dst, reg1, src2);
    }

    // slt (set on less than)
    public static void slt(Register dst, Register reg1, Register reg2) {
        Op("slt", dst, reg1, reg2);
    }

    // slti (set on less than immediate)
    public static void slti(Register dst, Register reg1, int imm) {
        Op("slti", dst, reg1, imm);
    }

    // sltu (set on less than unsigned)
    public static void sltu(Register dst, Register reg1, Register reg2) {
        Op("sltu", dst, reg1, reg2);
    }

    // sltiu (set on less than immediate unsigned)
    public static void sltiu(Register dst, Register reg1, int imm) {
        Op("sltiu", dst, reg1, imm);
    }

    // bnez (branch if not equal to zero)
    public static void bnez(Register reg1, String target) {
        Op("bnez", reg1, target);
    }

    // beqz (branch if equal to zero)
    public static void beqz(Register reg1, String target) {
        Op("beqz", reg1, target);
    }

    // `Op`s are helper functions for internal use.
    private static void Op(String instr, Register reg) {
        out.writeLine(instr + " " + reg.toString());
    }

    private static void Op(String instr, int imm) {
        out.writeLine(instr + " " + Integer.toString(imm));
    }

    private static void Op(String instr, String imm) {
        out.writeLine(instr + " " + imm);
    }

    private static void Op(String instr, Register reg1, Register reg2) {
        out.writeLine(instr + " " + reg1.toString() + ", " + reg2.toString());
    }

    private static void Op(String instr, Register reg, int imm) {
        out.writeLine(instr + " " + reg.toString() + ", " + Integer.toString(imm));
    }

    private static void Op(String instr, Register reg, String imm) {
        out.writeLine(instr + " " + reg.toString() + ", " + imm);
    }

    private static void Op(String instr, Register reg1, int offset, Register reg2) {
        out.writeLine(instr + " " + reg1.toString() + ", " + Integer.toString(offset) + "(" + reg2.toString() + ")");
    }

    private static void Op(String instr, Register reg1, Register reg2, String imm) {
        out.writeLine(instr + " " + reg1.toString() + ", " + reg2.toString() + ", " + imm);
    }

    private static void Op(String instr, Register reg1, Register reg2, int imm) {
        out.writeLine(instr + " " + reg1.toString() + ", " + reg2.toString() + ", " + Integer.toString(imm));
    }


    private static void Op(String instr, Register dst, Register reg1, Register reg2) {
        out.writeLine(instr + " " + dst.toString() + ", " + reg1.toString() + ", " + reg2.toString());
    }
}
