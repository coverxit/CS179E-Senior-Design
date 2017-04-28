import java.util.*;

public class Symbol {
    private String name;
    private static Map<String, Symbol> dict = new HashMap<>();

    private Symbol(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Symbol fromString(String n) {
        return dict.computeIfAbsent(n, k -> new Symbol(k));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Symbol))
            return false;

        Symbol rhs = (Symbol) obj;
        return rhs.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
