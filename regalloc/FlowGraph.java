package regalloc;

import cs132.vapor.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class FlowGraph {
    private List<FlowGraphNode> nodes = new ArrayList<>();
    private Map<FlowGraphNode, Set<FlowGraphNode>> edges = new HashMap<>();

    public FlowGraphNode newNode(VInstr instr, Set<String> def, Set<String> use) {
        FlowGraphNode gn = new FlowGraphNode(this, nodes.size(), instr, def, use);
        nodes.add(gn);
        return gn;
    }

    public FlowGraphNode getNode(int index) {
        return nodes.get(index);
    }

    public int getIndex(FlowGraphNode node) {
        return nodes.indexOf(node);
    }

    public List<FlowGraphNode> getNodes() {
        return new ArrayList<>(nodes);
    }

    public int nodesCount() {
        return nodes.size();
    }

    public void addEdge(FlowGraphNode from, FlowGraphNode to) {
        if (from != null && to != null && from != to && nodes.contains(from) && nodes.contains(to)) {
            edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            from.addSuccessor(to);
            to.addPredecessor(from);
        }
    }

    public Liveness computLiveness() {
        Map<FlowGraphNode, Set<String>> in = new LinkedHashMap<>();
        Map<FlowGraphNode, Set<String>> out = new LinkedHashMap<>();
        boolean updated;

        for (FlowGraphNode n : nodes) {
            in.put(n, new HashSet<>());
            out.put(n, new HashSet<>());
        }

        do {
            updated = false;

            for (FlowGraphNode n : nodes) {
                Set<String> oldin = new HashSet<>(in.get(n));
                Set<String> oldout = new HashSet<>(out.get(n));

                // in[n] = use[n]\/(out[n]-def[n])
                Set<String> newin = new HashSet<>(n.getUse());
                Set<String> diff = new HashSet<>(oldout);
                diff.removeAll(n.getDef());
                newin.addAll(diff);

                // out[n] = \/(s in succ[n]) in[s]
                Set<String> newout = new HashSet<>();
                for (FlowGraphNode s : n.getSucc())
                    newout.addAll(in.get(s));

                in.put(n, newin);
                out.put(n, newout);

                if (!newin.equals(oldin) || !newout.equals(oldout))
                    updated = true;
            }
        } while (updated);

        return new Liveness(new ArrayList<>(in.values()), new ArrayList<>(out.values()),
                nodes.stream().map(FlowGraphNode::getDef).collect(Collectors.toList()),
                nodes.stream().map(FlowGraphNode::getUse).collect(Collectors.toList()));
    }
}