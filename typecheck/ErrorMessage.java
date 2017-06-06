package typecheck;

import java.io.*;
import java.util.*;

public class ErrorMessage {
    private static boolean hasErrors = false;
    private static String fileName = "stdin";
    private static Map<Integer, List<String>> errors = new TreeMap<>();

    public static void setFileName(String n) {
        fileName = n;
    }

    public static void clearErrors() {
        errors.clear();
    }

    public static void complain(int lineNo, String msg) {
        hasErrors = true;

        StringBuilder sb = new StringBuilder();
        sb.append(fileName).append(" (").append(Integer.toString(lineNo)).append("): error: ");
        sb.append(msg);
        errors.computeIfAbsent(lineNo, l -> new ArrayList<>()).add(sb.toString());
    }

    public static void printErrors(PrintStream out) {
        errors.forEach((k, v) -> v.forEach(out::println));
    }

    public static boolean hasErrors() {
        return hasErrors;
    }
}
