package codegen;

import java.util.zip.Deflater;

public class VariableLabel {
    private final String base;
    private final int no;
    private final boolean deref;

    public VariableLabel(int n, String b) {
        no = n;
        base = b;
        deref = false;
    }

    public VariableLabel(int n) {
        no = n;
        base = null;
        deref = false;
    }

    public VariableLabel(String b) {
        no = -1;
        base = b;
        deref = false;
    }

    // Used for creating dereferred label
    private VariableLabel(VariableLabel other) {
        no = other.no;
        base = other.base;
        deref = true;
    }

    public VariableLabel dereference()
    {
        return new VariableLabel(this);
    }

    public boolean isDereference() { return deref; }

    @Override
    public String toString() {
        String lbl;

        if (no == -1)
            lbl = base;
        else if (base != null)
            lbl = base + "+" + Integer.toString(no);
        else
            lbl =  "t." + Integer.toString(no);

        if (deref)
            return "[" + lbl + "]";
        else
            return lbl;
    }
}
