package codegen;

import java.util.*;

public class ClassRecordManager {
    // Map class to its vtable
    // The inside map is to mapping method name to the override version.
    private Map<String, Map<String, String>> vtable = new LinkedHashMap<>();

    // Map class to its variables
    private Map<String, ArrayList<String>> variables = new LinkedHashMap<>();

    public void putMethod(String c, Map<String, String> a) {
        vtable.computeIfAbsent(c, k -> new LinkedHashMap<>()).putAll(a);
    }

    public void putVariable(String c, String v) {
        variables.computeIfAbsent(c, k -> new ArrayList<>()).add(v);
    }

    public int lookupMethodOffset(String c, String m) {
        return new ArrayList<>(vtable.get(c).keySet()).indexOf(m) * 4;
    }

    public int lookupVariableOffset(String c, String v) {
        // Since the child class can hold variables with the same name
        // inside its base class, we use lastIndexOf to get the one
        // in child class.
        // +4 for the vtable.
        return variables.get(c).lastIndexOf(v) * 4 + 4;
    }

    public int sizeOfClass(String c) {
        // +4 for the vtable.
        if (variables.containsKey(c))
            return variables.get(c).size() * 4 + 4;
        else
            return 4;
    }

    public Iterator<String> methodIterator(String c) {
        return vtable.get(c).values().iterator();
    }

    public Iterator<String> variableIterator(String c) {
        return variables.get(c).iterator();
    }
}
