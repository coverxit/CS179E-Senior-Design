package codegen;

import typecheck.*;

public class CodeGenPair {
    private final Scope scope;
    private final Translator translator;

    public CodeGenPair(Scope s, Translator t) {
        scope = s;
        translator = t;
    }

    public Scope getScope() {
        return scope;
    }

    public Translator getTranslator() {
        return translator;
    }
}
