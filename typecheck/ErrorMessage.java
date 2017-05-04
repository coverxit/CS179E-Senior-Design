package typecheck;

public class ErrorMessage {
    private static boolean errors = false;

    public static void complain(String msg) {
        errors = true;
        // System.out.println(msg);
    }

    public static boolean anyErrors() {
        return errors;
    }
}
