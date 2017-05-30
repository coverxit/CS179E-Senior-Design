package regalloc;

import cs132.vapor.ast.*;

import java.util.*;

public class Allocator {
    private RegisterPool pool;
    private List<Interval> active;
    private Map<String, Register> register;
    private Set<String> unusedParams;
    private Set<String> stack;

    public AllocationMap computeAllocation(List<Interval> ci, VVarRef.Local[] params) {
        pool = RegisterPool.CreateGlobalPool();
        active = new ArrayList<>();
        register = new LinkedHashMap<>();
        unusedParams = new HashSet<>();
        stack = new LinkedHashSet<>();

        List<Interval> intervals = new ArrayList<>(ci);
        // Sort by increasing start point
        intervals.sort(Comparator.comparingInt(Interval::getStart));

        // Map params to registers (in a0~a3 and `in` stack)
        for (int i = 0; i < params.length; i++) {
            String arg = params[i].ident;
            // If parameter is used during the function
            if (intervals.stream().map(Interval::getVar).anyMatch(o -> o.equals(arg))) {
                register.put(arg, pool.acquire());
                unusedParams.add(arg);
            }
        }

        for (Interval i : intervals) {
            expireOldInterval(i);

            // No need to allocate registers for the first parameters
            if (i.getStart() > 0 || !unusedParams.contains(i.getVar())) {
                if (!pool.hasFree()) {
                    spillAtInterval(i);
                } else {
                    register.put(i.getVar(), pool.acquire());
                    active.add(i);
                }
            }
        }

        return new AllocationMap(new LinkedHashMap<>(register),
                stack.toArray(new String[stack.size()]));
    }

    private void expireOldInterval(Interval interval) {
        // Sort by increasing end point
        active.sort(Comparator.comparingInt(Interval::getEnd));

        for (Iterator<Interval> iter = active.iterator(); iter.hasNext(); ) {
            Interval i = iter.next();
            if (i.getEnd() >= interval.getStart())
                return;

            iter.remove();
            pool.release(register.get(i.getVar()));

            // release the interval of first parameters
            if (unusedParams.contains(i.getVar()))
                unusedParams.remove(i.getVar());
        }
    }

    private void spillAtInterval(Interval interval) {
        // Sort by increasing end point
        active.sort(Comparator.comparingInt(Interval::getEnd));

        // Intervals for function parameters are marked as fixed. (They are not spilled)
        Interval spill = null;
        if (!active.isEmpty()) {
            int idx = active.size() - 1;
            do {
                spill = active.get(idx--);
            } while (idx >= 0 && unusedParams.contains(spill.getVar()));
            spill = idx < 0 ? null : spill;
        }

        if (spill != null && spill.getEnd() > interval.getEnd()) {
            register.put(interval.getVar(), register.get(spill.getVar()));
            register.remove(spill.getVar());
            stack.add(spill.getVar());
            active.remove(spill);
            active.add(interval);
        } else {
            stack.add(interval.getVar());
        }
    }
}

