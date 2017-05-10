package codegen;

import java.io.PrintStream;

public class Output {
    private static final String INDENT = "  "; // two spaces

    private String indent = "";
    private PrintStream stream;

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
        stream.print(indent + s);
    }

    public void writeLine(String s) {
        stream.println(indent + s);
    }

    public void writeLine() {
        stream.println();
    }
}
