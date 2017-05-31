package regalloc;

public class Register {
    // Callee-saved
    public static final Register s0 = new Register("s0");
    public static final Register s1 = new Register("s1");
    public static final Register s2 = new Register("s2");
    public static final Register s3 = new Register("s3");
    public static final Register s4 = new Register("s4");
    public static final Register s5 = new Register("s5");
    public static final Register s6 = new Register("s6");
    public static final Register s7 = new Register("s7");

    // Caller-saved
    public static final Register t0 = new Register("t0");
    public static final Register t1 = new Register("t1");
    public static final Register t2 = new Register("t2");
    public static final Register t3 = new Register("t3");
    public static final Register t4 = new Register("t4");
    public static final Register t5 = new Register("t5");
    public static final Register t6 = new Register("t6");
    public static final Register t7 = new Register("t7");
    public static final Register t8 = new Register("t8");
    public static final Register t9 = new Register("t9");

    // Argument passing
    public static final Register a0 = new Register("a0");
    public static final Register a1 = new Register("a1");
    public static final Register a2 = new Register("a2");
    public static final Register a3 = new Register("a3");

    // Return value/Temporary loading
    public static final Register v0 = new Register("v0");
    public static final Register v1 = new Register("v1");

    // Pointer registers
    public static final Register sp = new Register("sp");
    public static final Register fp = new Register("fp");
    public static final Register ra = new Register("ra");

    private final String reg;

    private Register(String r) {
        reg = r;
    }

    public boolean isCallerSaved() {
        return reg.startsWith("t");
    }

    public boolean isCalleeSaved() {
        return reg.startsWith("s");
    }

    public boolean isArgumentPassing() {
        return reg.startsWith("a");
    }

    public boolean isReturnOrLoading() {
        return reg.startsWith("v");
    }

    @Override
    public String toString() {
        return "$" + reg;
    }

    @Override
    public int hashCode() {
        return reg.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Register))
            return false;

        Register rhs = (Register) obj;
        return reg.equals(rhs.reg);
    }
}
