package typecheck;

import java.util.*;

public class SubtypingRelation {
    private static Map<Symbol, Set<Symbol>> relation = new LinkedHashMap<>();

    public static void insert(Symbol base, Symbol child) {
        relation.putIfAbsent(base, new LinkedHashSet<>());
        relation.get(base).add(child);

        // Subtyping is transitive
        for (Iterator<Symbol> it = relation.keySet().iterator(); it.hasNext(); ) {
            Symbol s = it.next();

            if (s.equals(base)) {
                Set<Symbol> children = relation.get(s);
                if (children.contains(base))
                    children.add(child);
            }
        }
    }

    public static boolean isSubtyping(Symbol child, Symbol base) {
        if (relation.containsKey(base))
            return relation.get(base).contains(child);
        else
            return false;
    }

    public static boolean contains(Symbol c) {
        return relation.containsKey(c);
    }
}
