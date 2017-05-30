package regalloc;

import java.util.*;
import java.util.stream.*;

public class AllocationMap {
    private final Map<String, Register> register;
    private final List<String> stack;
    private final int stackReserved;

    public AllocationMap(Map<String, Register> r, String[] s) {
        register = r;
        stack = Arrays.asList(s);
        stackReserved = usedCalleeRegister().size();
    }

    public List<Register> usedCalleeRegister() {
        return register.values().stream().filter(Register::isCalleeSaved).distinct().collect(Collectors.toList());
    }

    public Register lookupRegister(String s) {
        return register.getOrDefault(s, null);
    }

    public int lookupStack(String s) {
        int offset = stack.indexOf(s);
        return offset == -1 ? -1 : offset + stackReserved;
    }

    public int stackSize() {
        return stack.size() + stackReserved;
    }
}
