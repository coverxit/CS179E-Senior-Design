import syntaxtree.*;
import visitor.*;

public class Typecheck {
    public static void main(String[] args) throws ParseException {
        // According to the instruction: "java Typecheck < P.java"
        // We use `System.in` as the input stream.
        try {
            new MiniJavaParser(System.in);
            MiniJavaParser.Goal().accept(new DepthFirstVisitor());

            System.out.println("Program type checked successfully");
        } catch (ParseException e) {
            System.out.println("Type error");
        }
    }
}