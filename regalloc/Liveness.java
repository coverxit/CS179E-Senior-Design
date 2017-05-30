package regalloc;

import java.util.*;

public class Liveness {
    private final List<Set<String>> in;
    private final List<Set<String>> out;
    private final List<Set<String>> def;
    private final List<Set<String>> use;

    public Liveness(List<Set<String>> lsi, List<Set<String>> lso, List<Set<String>> lsd, List<Set<String>> lsu) {
        in = lsi;
        out = lso;
        def = lsd;
        use = lsu;
    }

    public List<Set<String>> getIn() {
        return new ArrayList<>(in);
    }

    public List<Set<String>> getOut() {
        return new ArrayList<>(out);
    }

    public List<Set<String>> getDef() {
        return new ArrayList<>(def);
    }

    public List<Set<String>> getUse() {
        return new ArrayList<>(use);
    }
}
