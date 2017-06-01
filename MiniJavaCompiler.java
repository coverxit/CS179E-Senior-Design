import parser.*;
import syntaxtree.*;
import typecheck.*;
import codegen.*;
import regalloc.*;
import asmgen.*;

import cs132.util.ProblemException;
import cs132.vapor.ast.VBuiltIn;
import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.parser.VaporParser;

import java.io.*;
import java.util.*;

public class MiniJavaCompiler {
    public static VaporProgram parseVapor(InputStream in) throws ProblemException, IOException {
        VBuiltIn.Op[] ops = {
                VBuiltIn.Op.Add, VBuiltIn.Op.Sub, VBuiltIn.Op.MulS, VBuiltIn.Op.Eq, VBuiltIn.Op.Lt, VBuiltIn.Op.LtS,
                VBuiltIn.Op.PrintIntS, VBuiltIn.Op.HeapAllocZ, VBuiltIn.Op.Error,
        };

        return VaporParser.run(new InputStreamReader(in), 1, 1, Arrays.asList(ops),
                // allowLocals, registers, allowStack
                true, null, false);
    }

    public static void main(String args[]) throws IOException {
        try {
            // Lexer & Parser
            new MiniJavaParser(System.in);
            Goal minijava = MiniJavaParser.Goal();
            Scope env = new Scope(minijava);

            // Type Checking (first pass)
            minijava.accept(new FirstPhaseVisitor(), env);
            if (!ErrorMessage.anyErrors()) {
                // Type Checking (second pass)
                minijava.accept(new SecondPhaseVisitor(), env);

                if (!ErrorMessage.anyErrors()) {
                    // Intermediate Code Generation
                    minijava.accept(new CodeGenVisitor(), new CodeGenPair(env, new Translator()));

                    V2VM.parseVapor(System.in);
                }
            }
        } catch (TokenMgrError e) {
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        } catch (ProblemException e) {
            System.out.println(e.getMessage());
        }
    }
}
