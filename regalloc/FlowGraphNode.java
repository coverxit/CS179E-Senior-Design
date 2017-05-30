package regalloc;

import cs132.vapor.ast.*;

import java.util.*;

public class FlowGraphNode {
    private final FlowGraph graph;
    private final int index;

    private final VInstr instr;
    private final Set<String> def;
    private final Set<String> use;

    private Set<FlowGraphNode> succ = new HashSet<>();
    private Set<FlowGraphNode> pred = new HashSet<>();

    public FlowGraphNode(FlowGraph g, int idx, VInstr vi, Set<String> d, Set<String> u) {
        graph = g;
        index = idx;
        instr = vi;
        def = d;
        use = u;
    }

    public FlowGraph getGraph() {
        return graph;
    }

    public int getIndex() {
        return index;
    }

    public Set<FlowGraphNode> getSucc() {
        return new HashSet<>(succ);
    }

    public Set<FlowGraphNode> getPred() {
        return new HashSet<>(pred);
    }

    public VInstr getInstr() {
        return instr;
    }

    public Set<String> getDef() {
        return new HashSet<>(def);
    }

    public Set<String> getUse() {
        return new HashSet<>(use);
    }

    public void addSuccessor(FlowGraphNode gn) {
        if (gn != null && this != gn)
            succ.add(gn);
    }

    public void addPredecessor(FlowGraphNode gn) {
        if (gn != null && this != gn)
            pred.add(gn);
    }
}
