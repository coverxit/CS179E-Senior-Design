package codegen;

import java.io.PrintStream;

public class Output {
    private static final String INDENT = "  "; // two spaces

    private String indent = "";
    private PrintStream stream;
    private boolean newLine = true;

    public Output(PrintStream s) {
        stream = s;
    }

    public void increaseIndent() {
        indent += INDENT;
    }

    public void decreaseIndent() {
        indent = indent.substring(0, indent.length() - INDENT.length());
    }

    public void setOutputStream(PrintStream s) {
        stream = s;
    }

    public void write(String s) {
        stream.print((newLine ? indent : "") + s);
        newLine = false;
    }

    public void writeLine(String s) {
        stream.println((newLine ? indent : "") + s);
        newLine = true;
    }

    public void writeLine() {
        stream.println();
    }
}
