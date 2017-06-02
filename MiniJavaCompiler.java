import parser.*;
import syntaxtree.*;
import typecheck.*;
import codegen.*;
import regalloc.*;
import asmgen.*;

import cs132.util.*;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.*;

public class MiniJavaCompiler {
    private static void printMessage(String message) {
        System.out.println("minijavac: " + message);
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp("minijavac [options] <source files>", options);
        System.exit(1);
    }

    public static void compile(String file, boolean v, boolean vm, boolean asm) {
        try {
            InputStream in = new FileInputStream(file);
            String rawName = FilenameUtils.removeExtension(file);

            // Lexer & Parser
            new MiniJavaParser(in);
            Goal minijava = MiniJavaParser.Goal();
            ErrorMessage.setOutput(System.err);

            // Phase 1-1: Type Checking (first pass)
            Scope env = new Scope(minijava);
            minijava.accept(new FirstPhaseVisitor(), env);
            if (!ErrorMessage.anyErrors()) {
                // Phase 1-2: Type Checking (second pass)
                minijava.accept(new SecondPhaseVisitor(), env);

                if (!ErrorMessage.anyErrors()) {
                    // Phase 2: Intermediate Code Generation
                    ByteArrayOutputStream vapor = new ByteArrayOutputStream();
                    minijava.accept(new CodeGenVisitor(), new CodeGenPair(env, new Translator(new PrintStream(vapor))));
                    if (v) new FileOutputStream(rawName + ".vapor").write(vapor.toByteArray());

                    // Phase 3: Register Allocation
                    ByteArrayOutputStream vaporM = V2VM.V2VM(new ByteArrayInputStream(vapor.toByteArray()));
                    if (vm) new FileOutputStream(rawName + ".vaporm").write(vaporM.toByteArray());

                    // Phase 4: Instruction Selection
                    ByteArrayOutputStream mips = VM2M.VM2M(new ByteArrayInputStream(vaporM.toByteArray()));
                    if (asm) new FileOutputStream(rawName + ".s").write(mips.toByteArray());
                }
            }
        } catch (TokenMgrError | parser.ParseException e) {
            System.out.println(e.getMessage());
        } catch (ProblemException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Options options = new Options();
        options.addOption("v", "vapor", false,
                "Run the intermediate code generation stage, producing the corresponding Vapor code file.");
        options.addOption("vm", "vaporM", false,
                "Run the register allocation stage, producing the corresponding VaporM code file.");
        options.addOption("asm", "assembly", false,
                "(Default) Run the instruction selection stage, producing the corresponding MIPS assembly file.");

        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            List<String> files = cmd.getArgList();

            if (files.isEmpty()) {
                printMessage("no source files");
                printHelp(options);
            } else {
                files.stream().filter(f -> !new File(f).isFile()).findFirst().ifPresent(f -> {
                    printMessage("file not found: " + f);
                    printHelp(options);
                });

                boolean asm = cmd.hasOption("asm") || (!cmd.hasOption("v") && !cmd.hasOption("vm"));
                files.forEach(f -> compile(f, cmd.hasOption("v"), cmd.hasOption("vm"), asm));
            }
        } catch (org.apache.commons.cli.ParseException e) {
            // lower the first letter, which as default is an upper letter.
            printMessage(e.getMessage().substring(0, 1).toLowerCase() + e.getMessage().substring(1));
            printHelp(options);
        }
    }
}
