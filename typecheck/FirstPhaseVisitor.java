package typecheck;

import java.util.*;

import syntaxtree.*;
import visitor.*;

/**
 * The first pass builds the symbol table, checks overloading and all distinct properties.
 * Also the first pass analyze the class definitions, allowing child classes defined before their base classes.
 */
public class FirstPhaseVisitor extends GJVoidDepthFirst<Scope> {
    /*
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    @Override
    public void visit(Goal n, Scope s) {
        // Build a list of all classes
        NodeList cl = new NodeList(n.f0);
        for (Enumeration<Node> e = n.f1.elements(); e.hasMoreElements(); ) {
            cl.addNode(e.nextElement());
        }

        // distinct(classname(mc),classname(d1),...,classname(dn))
        TypeCheckHelper.classDistinct(cl).stream().map(d -> new Object() {
            int line = TypeCheckHelper.identifierLine(d);
            String name = TypeCheckHelper.identifierName(d);
        }).forEach(it -> ErrorMessage.complain(it.line, "duplicate class: " + it.name));

        n.f0.accept(this, s);
        n.f1.accept(this, s);
    }

    /*
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public void visit(MainClass n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        SubtypingRelation.insert(id, id); // Reflexive

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // Method `public static void main(String[] id)` is ignored for overloading check,
        // since we all know parser would complain if some other classes defined this method.
        // distinct(id1,...,idr)
        TypeCheckHelper.variableDistinct(n.f14).stream().map(d -> new Object() {
            int line = TypeCheckHelper.identifierLine(d);
            String name = TypeCheckHelper.identifierName(d);
        }).forEach(it -> ErrorMessage.complain(it.line,
                "variable " + it.name + " is already defined in method main(String[])"));

        n.f14.accept(this, ns);
        // Skip `( Statement() )*`
    }

    /*
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public void visit(ClassDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        SubtypingRelation.insert(id, id);

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // distinct(id1,...,idf)
        TypeCheckHelper.variableDistinct(n.f3).stream().map(d -> new Object() {
            int line = TypeCheckHelper.identifierLine(d);
            String name = TypeCheckHelper.identifierName(d);
        }).forEach(it -> ErrorMessage.complain(it.line,
                "variable " + it.name + " is already defined in class " + id.toString()));

        // distinct(methodname(m1),...methodname(mk))
        TypeCheckHelper.methodDistinct(n.f4).stream().map(d -> new Object() {
            int line = TypeCheckHelper.identifierLine(d);
            String name = TypeCheckHelper.identifierName(d);
        }).forEach(it -> ErrorMessage.complain(it.line,
                "method " + it.name + " is already defined in class " +
                        id.toString() + " (overloading is not allowed)"));

        n.f3.accept(this, ns);
        n.f4.accept(this, ns);
    }

    /*
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public void visit(ClassExtendsDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.className(n));
        SubtypingRelation.insert(id, id);

        Symbol base = Symbol.fromString(TypeCheckHelper.identifierName(n.f3));
        Binder b = s.lookup(base);
        if (b != null) {
            SubtypingRelation.insert(base, id);

            Scope ns = new Scope(b.getScope(), n);
            s.add(id, n, ns);

            // distinct(id1,...,idf)
            TypeCheckHelper.variableDistinct(n.f5).stream().map(d -> new Object() {
                int line = TypeCheckHelper.identifierLine(d);
                String name = TypeCheckHelper.identifierName(d);
            }).forEach(it -> ErrorMessage.complain(it.line,
                    "variable " + it.name + " is already defined in class " + id.toString()));

            // distinct(methodname(m1),...methodname(mk))
            TypeCheckHelper.methodDistinct(n.f6).stream().map(d -> new Object() {
                int line = TypeCheckHelper.identifierLine(d);
                String name = TypeCheckHelper.identifierName(d);
            }).forEach(it -> ErrorMessage.complain(it.line,
                    "method " + it.name + " is already defined in class " +
                            id.toString() + " (overloading is not allowed)"));

            n.f5.accept(this, ns);
            n.f6.accept(this, ns);
        } else {
            ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f3),
                    "cannot find symbol: class " + TypeCheckHelper.identifierName(n.f3));
        }
    }

    /*
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public void visit(MethodDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.methodName(n));
        Binder b = s.lookupParent(id);
        MethodType type = TypeCheckHelper.methodType(n);

        // Overloading check
        // noOverloading(id,idp,methodname(mi))
        if (b != null) {
            MethodType base = TypeCheckHelper.methodType((MethodDeclaration) b.getType());

            // Guarantee only overriding
            if (!base.equals(type)) {
                ErrorMessage.complain(TypeCheckHelper.identifierLine(n.f2),
                        "method " + TypeCheckHelper.className(s.getNodeBound()) + "." +
                                TypeCheckHelper.methodName(n)  + type.getSignature() + " differs from " +
                                TypeCheckHelper.className(b.getScope().getParent().getNodeBound()) + "." +
                                TypeCheckHelper.methodName(n) + base.getSignature() + " in signature " +
                                "(overloading is not allowed)");
            }
        }

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        if (n.f4.present()) {
            // distinct(idf1,...,idfn)
            TypeCheckHelper.parameterDistinct((FormalParameterList) n.f4.node).stream().map(d -> new Object() {
                int line = TypeCheckHelper.identifierLine(d);
                String name = TypeCheckHelper.identifierName(d);
            }).forEach(it -> ErrorMessage.complain(it.line,
                    "parameter " + it.name + " is already defined in method " + id.toString()));

            n.f4.accept(this, ns);
        }

        // distinct(id1,...,idr)
        TypeCheckHelper.variableDistinct(n.f7).stream().map(d -> new Object() {
            int line = TypeCheckHelper.identifierLine(d);
            String name = TypeCheckHelper.identifierName(d);
        }).forEach(it -> ErrorMessage.complain(it.line,
                "variable " + it.name + " is already defined in method " + id.toString() + type.getSignature()));
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public void visit(FormalParameter n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f1));

        // Type check for VarDeclaration is in second phase.
        s.add(id, n, s);
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public void visit(VarDeclaration n, Scope s) {
        Symbol id = Symbol.fromString(TypeCheckHelper.identifierName(n.f1));

        // Type check for VarDeclaration is in second phase.
        s.add(id, n, s);
    }
}
