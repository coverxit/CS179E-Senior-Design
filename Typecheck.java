import syntaxtree.*;

public class Typecheck {
    public static void main(String[] args) {
        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);
            Goal program = MiniJavaParser.Goal();
            Scope env = new Scope(program);

            program.accept(new FirstPhaseVisitor(), env);
            if (ErrorMessage.anyErrors()) {
                System.out.println("Type error");
                System.exit(1);
            } else {
                program.accept(new SecondPhaseVisitor(), env);
                if (ErrorMessage.anyErrors()) {
                    System.out.println("Type error");
                    System.exit(1);
                } else {
                    System.out.println("Program type checked successfully");
                }
            }
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}