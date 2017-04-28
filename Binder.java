import syntaxtree.*;

public class Binder {
    private Symbol symbol;
    private Node type;
    private Scope scope;

    public Binder(Symbol sy, Node t, Scope nsc) {
        symbol = sy;
        type = t;
        scope = nsc;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Node getType() {
        return type;
    }

    public Scope getScope() {
        return scope;
    }
}
