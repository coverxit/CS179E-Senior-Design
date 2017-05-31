package asmgen;

import regalloc.Register;

public class AsmGenHelper {
    // li (load immediate)
    public static String li(Register reg, int imm) {
        return Op("li", reg, imm);
    }

    // syscall (system call)
    public static String syscall() {
        return "syscall";
    }

    private static String Op(String instr, Register reg1, Register reg2) {
        return instr + " " + reg1.toString() + " " + reg2.toString();
    }

    private static String Op(String instr, Register reg, int imm) {
        return instr + " " + reg.toString() + " " + Integer.toString(imm);
    }
}
