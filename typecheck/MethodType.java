package typecheck;

import java.util.*;

public class MethodType {
    private final ArrayList<String> parameters;
    private final ExpressionType returnType;

    public MethodType(ArrayList<String> p, ExpressionType rt) {
        parameters = new ArrayList<>(p);
        returnType = rt;
    }

    public ArrayList<String> getParameters() {
        return parameters;
    }

    public ExpressionType getReturnType() {
        return returnType;
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder("(");
        if (!parameters.isEmpty()) {
            parameters.forEach(p -> sb.append(p).append(", "));
            sb.delete(sb.length() - 2, sb.length());
        }
        return sb.append(")").toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MethodType))
            return false;

        MethodType rhs = (MethodType) obj;
        return rhs.parameters.equals(this.parameters)
                && rhs.returnType.equals(this.returnType);
    }
}
