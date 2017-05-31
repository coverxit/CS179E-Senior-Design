import regalloc.*;

import cs132.util.*;
import cs132.vapor.parser.*;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;

import java.io.*;
import java.util.*;

public class VM2M {
    public static VaporProgram parseVapor(InputStream in) throws ProblemException, IOException {
        Op[] ops = {
                Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
                Op.PrintIntS, Op.HeapAllocZ, Op.Error,
        };

        String[] registers = {
                "v0", "v1",
                "a0", "a1", "a2", "a3",
                "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                "t8",
        };

        return VaporParser.run(new InputStreamReader(in), 1, 1, Arrays.asList(ops),
                // allowLocals, registers, allowStack
                true, registers, false);
    }

    public static void main(String[] args) throws ProblemException, IOException {

    }
}
