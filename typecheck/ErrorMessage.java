package typecheck;

import java.io.*;

public class ErrorMessage {
    private static boolean errors = false;
    private static PrintStream out = System.out;

    public static void setOutput(PrintStream s) {
        out = s;
    }

    public static void complain(String msg) {
        errors = true;
        out.println(msg);
    }

    public static boolean anyErrors() {
        return errors;
    }
}
