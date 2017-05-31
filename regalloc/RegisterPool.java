package regalloc;

import java.util.*;

public class RegisterPool {
    private Set<Register> all = new LinkedHashSet<>();
    private Set<Register> use = new HashSet<>();

    private RegisterPool(Register[] regs) {
        Collections.addAll(all, regs);
    }

    // We only use t0~t7 and s0~s7. a0~a3, v0 and v1 are reserved.
    public static RegisterPool CreateGlobalPool() {
        Register[] regs = {
                // Caller-saved
                Register.t0, Register.t1, Register.t2, Register.t3,
                Register.t4, Register.t5, Register.t6, Register.t7,
                Register.t8,
                // Callee-saved
                Register.s0, Register.s1, Register.s2, Register.s3,
                Register.s4, Register.s5, Register.s6, Register.s7
        };

        return new RegisterPool(regs);
    }

    // Local pool used for retrieving values from `local` stack
    public static RegisterPool CreateLocalPool() {
        Register[] regs = {
                Register.v0, Register.v1,
                Register.a0, Register.a1, Register.a2, Register.a3
        };

        return new RegisterPool(regs);
    }

    public boolean contains(Register reg) {
        return all.contains(reg);
    }

    public boolean inUse(Register reg) {
        return use.contains(reg);
    }

    public boolean hasFree() {
        return all.size() > use.size();
    }

    public Register acquire() {
        Register ret = null;
        Set<Register> diff = new LinkedHashSet<>(all);
        diff.removeAll(use);

        if (!diff.isEmpty()) {
            ret = diff.iterator().next();
            use.add(ret);
        }
        return ret;
    }

    public void release(Register reg) {
        use.remove(reg);
    }
}
