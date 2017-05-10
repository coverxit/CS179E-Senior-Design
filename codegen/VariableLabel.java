package codegen;

public class VariableLabel {
    private final String base;
    private final int no;
    private final boolean deref;
    private final boolean call;

    private VariableLabel(int n, String b, boolean d, boolean c) {
        no = n;
        base = b;
        deref = d;
        call = c;
    }

    // Used for creating dereferred label
    private VariableLabel(VariableLabel other) {
        no = other.no;
        base = other.base;
        deref = true;
        call = false;
    }

    public VariableLabel dereference()
    {
        return new VariableLabel(this);
    }

    public boolean isDereference() {
        return deref;
    }

    public boolean isFunctionCall() {
        return call;
    }

    public boolean isThisPointer() {
        return base != null && base.equals("this") && no == -1 && !deref && !call;
    }

    public static VariableLabel TempVariable(int n) {
        return new VariableLabel(n, null, false, false);
    }

    public static VariableLabel FunctionCall(String fc) {
        return new VariableLabel(-1, fc, false, true);
    }

    public static VariableLabel Constant(String c) {
        return new VariableLabel(-1, c, false, false);
    }

    public static VariableLabel This() {
        return This(-1);
    }

    public static VariableLabel This(int offset) {
        return new VariableLabel(offset, "this", false, false);
    }

    public static VariableLabel Local(String l) {
        return Local(-1, l);
    }

    public static VariableLabel Local(int offset, String l) {
        return new VariableLabel(offset, l, false, false);
    }

    @Override
    public String toString() {
        String lbl;

        if (no == -1)
            lbl = base;
        else if (base != null)
            lbl = base + "+" + Integer.toString(no);
        else
            lbl =  "t." + Integer.toString(no);

        if (deref && !call)
            return "[" + lbl + "]";
        else
            return lbl;
    }
}
