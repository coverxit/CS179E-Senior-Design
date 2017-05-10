package codegen;

public class JumpLabel {
    private final String format;
    private final int no;

    private JumpLabel(String f, int n) {
        format = f;
        no = n;
    }

    public static JumpLabel Null(int n) {
        return new JumpLabel("null%d", n);
    }

    public static JumpLabel IfElse(int n) {
        return new JumpLabel("if%d_else", n);
    }

    public static JumpLabel IfEnd(int n) {
        return new JumpLabel("if%d_end", n);
    }

    // Boundary check
    public static JumpLabel Bounds(int n) {
        return new JumpLabel("bounds%d", n);
    }

    public static JumpLabel WhileTop(int n) {
        return new JumpLabel("while%d_top", n);
    }

    public static JumpLabel WhileEnd(int n) {
        return new JumpLabel("while%d_end", n);
    }

    public static JumpLabel AndElse(int n) {
        return new JumpLabel("and%d_else", n);
    }

    public static JumpLabel AndEnd(int n) {
        return new JumpLabel("and%d_end", n);
    }

    @Override
    public String toString() {
        return String.format(format, no);
    }
}
