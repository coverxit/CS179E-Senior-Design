import java.util.*;

import syntaxtree.*;
import visitor.*;

/**
 * The first pass builds the symbol table, checks overloading and all distinct properties.
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
        if (Helper.classDistinct(cl)) {
            n.f0.accept(this, s);
            n.f1.accept(this, s);
        } else {
            ErrorMessage.complain("Goal: Class identifiers are not pairwise distinct.");
        }
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id); // Reflexive

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // Method `public static void main(String[] id)` is ignored for overloading check,
        // since we all know parser would complain if some other classes defined this method.
        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f14)) {
            n.f14.accept(this, ns);
            // Skip `( Statement() )*`
        } else {
            ErrorMessage.complain("MainClass: Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id);

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        // distinct(id1,...,idf)
        if (Helper.variableDistinct(n.f3)) {
            n.f3.accept(this, ns);

            // distinct(methodname(m1),...methodname(mk))
            if (Helper.methodDistinct(n.f4))
                n.f4.accept(this, ns);
            else
                ErrorMessage.complain("ClassDeclaration: Overloading is not allowed. " +
                        "In class " + id.toString());
        } else {
            ErrorMessage.complain("ClassDeclaration: Variable identifiers are not pairwise distinct. " +
                    "In class " + id.toString());
        }
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
        Symbol id = Symbol.fromString(Helper.className(n));
        SubtypingRelation.insert(id, id);

        Symbol base = Symbol.fromString(Helper.identifierName(n.f3));
        Binder b = s.lookup(base);
        if (b != null) {
            SubtypingRelation.insert(base, id);

            Scope ns = new Scope(b.getScope(), n);
            s.add(id, n, ns);

            // distinct(id1,...,idf)
            if (Helper.variableDistinct(n.f5)) {
                n.f5.accept(this, ns);

                // distinct(methodname(m1),...methodname(mk))
                if (Helper.methodDistinct(n.f6))
                    n.f6.accept(this, ns);
                else
                    ErrorMessage.complain("ClassExtendsDeclaration: Overloading is not allowed. " +
                            "In class " + id.toString());
            } else {
                ErrorMessage.complain("ClassExtendsDeclaration: Variable identifiers are not pairwise distinct. " +
                        "In class " + id.toString());
            }
        } else {
            ErrorMessage.complain("ClassExtendsDeclaration: Base class '" + base.toString() + "' is not defined. " +
                    "In class " + id.toString());
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
        Symbol id = Symbol.fromString(Helper.methodName(n));
        Binder b = s.lookup(id);

        // Overloading check
        // noOverloading(id,idp,methodname(mi))
        if (b != null) {
            MethodType base = Helper.methodType((MethodDeclaration) b.getType());
            MethodType inherit = Helper.methodType(n);

            // Guarantee only overriding
            if (!base.equals(inherit)) {
                ErrorMessage.complain("MethodDeclaration: Overloading is not allowed. " +
                        "Method: " + id.toString());
            }
        }

        Scope ns = new Scope(s, n);
        s.add(id, n, ns);

        if (n.f4.present()) {
            // distinct(idf1,...,idfn)
            if (Helper.parameterDistinct((FormalParameterList) n.f4.node)) {
                n.f4.accept(this, ns);
            } else {
                ErrorMessage.complain("MethodDeclaration: Parameter identifiers are not pairwise distinct. " +
                        "In method " + id.toString());
            }
        }

        // distinct(id1,...,idr)
        if (Helper.variableDistinct(n.f7)) {
            n.f7.accept(this, ns);
        } else {
            ErrorMessage.complain("MethodDeclaration: Variable identifiers are not pairwise distinct. " +
                    "In method " + id.toString());
        }
    }

    /*
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public void visit(FormalParameter n, Scope s) {
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));

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
        Symbol id = Symbol.fromString(Helper.identifierName(n.f1));

        // Type check for VarDeclaration is in second phase.
        s.add(id, n, s);
    }
}
