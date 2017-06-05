package typecheck;

import java.util.*;

import syntaxtree.*;

public class Scope {
    private Map<Symbol, Binder> table = new LinkedHashMap<>();
    private Scope parent;
    private Node bind;

    public Scope(Node b) {
        parent = null;
        bind = b;
    }

    public Scope(Scope p, Node b) {
        parent = p;
        bind = b;
    }

    public void add(Symbol sy, Node t, Scope sc) {
        table.put(sy, new Binder(sy, t, sc));
    }

    public Binder lookup(Symbol s) {
        Binder b = table.get(s);

        if (b != null)
            return b;
        // Lookup recursively
        else if (parent != null)
            return parent.lookup(s);
        else
            return null;
    }

    public Binder lookupLocal(Symbol s) {
        return table.get(s);
    }

    public Binder lookupParent(Symbol s) {
        if (parent != null)
            return parent.lookup(s);
        else
            return null;
    }

    public Iterator<Binder> symbolIterator() {
        return table.values().iterator();
    }

    public Scope getParent() {
        return parent;
    }

    public Node getNodeBound() {
        return bind;
    }
}
