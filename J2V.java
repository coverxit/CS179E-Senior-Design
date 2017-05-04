import syntaxtree.*;
import typecheck.*;

public class J2V {
    public static void main(String args[]) throws ParseException {
        new MiniJavaParser(System.in);
        Goal program = MiniJavaParser.Goal();
        Scope env = new Scope(program);

        program.accept(new FirstPhaseVisitor(), env);
    }
}
