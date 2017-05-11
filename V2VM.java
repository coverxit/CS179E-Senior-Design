import cs132.util.*;
import cs132.vapor.parser.*;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;

import java.io.*;

public class V2VM {
    public static VaporProgram parseVapor(InputStream in) throws ProblemException, IOException {
        Op[] ops = {
                Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
                Op.PrintIntS, Op.HeapAllocZ, Op.Error,
        };

        return VaporParser.run(new InputStreamReader(in), 1, 1, java.util.Arrays.asList(ops),
                    // allowLocals, registers, allowStack
                    true, null, false);
    }

    public static void main(String[] args) throws ProblemException, IOException {
        VaporProgram program = parseVapor(System.in);
    }
}
