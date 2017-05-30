package regalloc;

import codegen.Output;
import cs132.vapor.ast.*;

import java.util.*;

public class RegAllocHelper {
    public static FlowGraph generateFlowGraph(VFunction func) {
        FlowGraph graph = new FlowGraph();
        List<FlowGraphNode> nodes = new ArrayList<>();

        for (VInstr instr : func.body) {
            Set<String> def = new HashSet<>();
            Set<String> use = new HashSet<>();

            instr.accept(new VInstr.Visitor<RuntimeException>() {
                @Override
                public void visit(VAssign vAssign) {
                    def.add(vAssign.dest.toString());
                    if (vAssign.source instanceof VVarRef) {
                        use.add(vAssign.source.toString());
                    }
                }

                @Override
                public void visit(VCall vCall) {
                    def.add(vCall.dest.toString());
                    for (VOperand arg : vCall.args) {
                        if (arg instanceof VVarRef) {
                            use.add(arg.toString());
                        }
                    }
                }

                @Override
                public void visit(VBuiltIn vBuiltIn) {
                    if (vBuiltIn.dest != null)
                        def.add(vBuiltIn.dest.toString());

                    for (VOperand arg : vBuiltIn.args) {
                        if (arg instanceof VVarRef) {
                            use.add(arg.toString());
                        }
                    }
                }

                @Override
                public void visit(VMemWrite vMemWrite) {
                    VMemRef.Global ref = (VMemRef.Global) vMemWrite.dest;
                    def.add(ref.base.toString());
                    if (vMemWrite.source instanceof VVarRef) {
                        use.add(vMemWrite.source.toString());
                    }
                }

                @Override
                public void visit(VMemRead vMemRead) {
                    def.add(vMemRead.dest.toString());

                    VMemRef.Global ref = (VMemRef.Global) vMemRead.source;
                    use.add(ref.base.toString());
                }

                @Override
                public void visit(VBranch vBranch) {
                    if (vBranch.value instanceof VVarRef) {
                        use.add(vBranch.value.toString());
                    }

                    // the branch target is label, thus no use produced.
                }

                @Override
                public void visit(VGoto vGoto) {
                    // vGoto produces no def and use.
                }

                @Override
                public void visit(VReturn vReturn) {
                    if (vReturn.value != null) {
                        if (vReturn.value instanceof VVarRef) {
                            use.add(vReturn.value.toString());
                        }
                    }
                }
            });

            nodes.add(graph.newNode(instr, def, use));
        }

        for (int i = 0; i < func.body.length; i++) {
            VInstr instr = func.body[i];
            FlowGraphNode prev = i > 0 ? nodes.get(i - 1) : null;
            FlowGraphNode cur = nodes.get(i);

            // Edge from the prev instr to current instr.
            if (prev != null)
                graph.addEdge(prev, cur);

            if (instr instanceof VBranch) {
                VLabelRef<VCodeLabel> target = ((VBranch) instr).target;
                FlowGraphNode to = nodes.get(target.getTarget().instrIndex);
                graph.addEdge(cur, to);
            } else if (instr instanceof VGoto) {
                // For gotos, we only allow goto labels.
                VLabelRef<VCodeLabel> target = ((VAddr.Label<VCodeLabel>) ((VGoto) instr).target).label;
                FlowGraphNode to = nodes.get(target.getTarget().instrIndex);
                graph.addEdge(cur, to);
            }
        }

        return graph;
    }

    public static List<Interval> generateLiveIntervals(FlowGraph graph, Liveness liveness) {
        Map<String, Interval> intervals = new HashMap<>();

        List<Set<String>> actives = new ArrayList<>();
        for (FlowGraphNode n : graph.getNodes()) {
            // active[n] = def[n] \/ in[n]
            Set<String> active = new HashSet<>(n.getDef());
            active.addAll(liveness.getIn().get(n.getIndex()));
            actives.add(active);
        }

        for (int i = 0; i < actives.size(); i++) {
            for (String var : actives.get(i)) {
                if (intervals.containsKey(var)) { // update end
                    intervals.get(var).setEnd(i);
                } else { // create new interval
                    intervals.put(var, new Interval(var, i, i));
                }
            }
        }

        return new ArrayList<>(intervals.values());
    }

    public static String in(int offset) {
        return "in[" + Integer.toString(offset) + "]";
    }

    public static String out(int offset) {
        return "out[" + Integer.toString(offset) + "]";
    }

    public static String local(int offset) {
        return "local[" + Integer.toString(offset) + "]";
    }

    public static String memoryReference(Register reg, int offset) {
        if (offset > 0)
            return "[" + reg.toString() + "+" + Integer.toString(offset) + "]";
        else
            return "[" + reg.toString() + "]";
    }
}
