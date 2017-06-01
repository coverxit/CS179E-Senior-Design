package regalloc;

import cs132.util.*;
import cs132.vapor.parser.*;
import cs132.vapor.ast.*;
import cs132.vapor.ast.VBuiltIn.Op;

import java.io.*;
import java.util.*;

public class V2VM {
    public static VaporProgram parseVapor(InputStream in) throws ProblemException, IOException {
        Op[] ops = {
                Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
                Op.PrintIntS, Op.HeapAllocZ, Op.Error,
        };

        return VaporParser.run(new InputStreamReader(in), 1, 1, Arrays.asList(ops),
                    // allowLocals, registers, allowStack
                    true, null, false);
    }

    public static void main(String[] args) throws ProblemException, IOException {
        Allocator allocator = new Allocator();
        Converter converter = new Converter();
        VaporProgram program = parseVapor(System.in);

        converter.outputConstSegment(program.dataSegments);
        for (VFunction func : program.functions) {
            FlowGraph graph = RegAllocHelper.generateFlowGraph(func);
            Liveness liveness = graph.computLiveness();

            // Register allocation is applied to ech function separately.
            List<Interval> intervals = RegAllocHelper.generateLiveIntervals(graph, liveness);
            AllocationMap map = allocator.computeAllocation(intervals, func.params);
            converter.outputFunction(func, map, liveness);
            converter.getOutput().writeLine();
        }
    }
}
