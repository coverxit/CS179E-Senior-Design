package regalloc;

public class Interval {
    private final String var;
    private int start;
    private int end;

    public Interval(String v, int s, int e) {
        var = v;
        start = s;
        end = e;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int e) {
        end = e;
    }

    public String getVar() {
        return var;
    }
}
