import syntaxtree.*;

public class ExpressionType {
    private final Node node;
    private final String type;

    public ExpressionType(Node n, String t) {
        node = n;
        type = t;
    }

    public Node getNode() {
        return node;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ExpressionType))
            return false;

        ExpressionType rhs = (ExpressionType) obj;
        return rhs.type.equals(this.type);
    }
}
