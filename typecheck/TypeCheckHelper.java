package typecheck;

import java.util.*;

import syntaxtree.*;

public class TypeCheckHelper {
    public static String className(Node c) {
        if (c instanceof MainClass) {
            /*
             * Grammar production:
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
            return TypeCheckHelper.identifierName(((MainClass) c).f1);
        } else if (c instanceof ClassDeclaration) {
            /*
             * Grammar production:
             * f0 -> "class"
             * f1 -> Identifier()
             * f2 -> "{"
             * f3 -> ( VarDeclaration() )*
             * f4 -> ( MethodDeclaration() )*
             * f5 -> "}"
             */
            return TypeCheckHelper.identifierName(((ClassDeclaration) c).f1);
        } else { // c instanceof ClassExtendsDeclaration
            /*
             * Grammar production:
             * f0 -> "class"
             * f1 -> Identifier()
             * f2 -> "extends"
             * f3 -> Identifier()
             * f4 -> "{"
             * f5 -> ( VarDeclaration() )*
             * f6 -> ( MethodDeclaration() )*
             * f7 -> "}"
             */
            return TypeCheckHelper.identifierName(((ClassExtendsDeclaration) c).f1);
        }
    }

    public static String methodName(Node m) {
        /*
         * Grammar production:
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
        return TypeCheckHelper.identifierName(((MethodDeclaration) m).f2);
    }

    public static String identifierName(Identifier id) {
        return id.f0.tokenImage;
    }

    public static String getParameters(FormalParameterList fpl)
    {
	String params = "";
	params = params + identifierName(fpl.f0.f1);
	return params;
    }

    public static boolean parameterDistinct(FormalParameterList fpl) {
        // Check if FormalParameterList() are pairwise distinct
        Set<String> declared = new HashSet<>();

        /*
         * Grammar production:
         * f0 -> FormalParameter()
         * f1 -> ( FormalParameterRest() )*
         */
        FormalParameter first = fpl.f0;
        NodeListInterface rest = fpl.f1;

        /*
         * Grammar production:
         * f0 -> Type()
         * f1 -> Identifier()
         */
        declared.add(TypeCheckHelper.identifierName(first.f1));
        for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
             * f0 -> ","
             * f1 -> FormalParameter()
             */
            String id = TypeCheckHelper.identifierName(((FormalParameterRest) e.nextElement()).f1.f1);

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }
        return true;
    }

    public static boolean variableDistinct(NodeListInterface vds) {
        // Checks if ( VarDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = vds.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             * f2 -> ";"
             */
            String id = TypeCheckHelper.identifierName(((VarDeclaration) e.nextElement()).f1);

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean methodDistinct(NodeListInterface mds) {
        // Checks if ( MethodDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = mds.elements(); e.hasMoreElements(); ) {
            /*
             * Grammar production:
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
            String id = TypeCheckHelper.methodName(e.nextElement());

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static boolean classDistinct(NodeListInterface tds) {
        // Checks if ( TypeDeclaration() )* are pairwise distinct
        Set<String> declared = new HashSet<>();

        for (Enumeration<Node> e = tds.elements(); e.hasMoreElements(); ) {
            String id;
            Node n = e.nextElement();

            if (n instanceof MainClass) {
                /*
                 * Grammar production:
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
                id = TypeCheckHelper.className(n);
            } else { // n instanceof TypeDeclaration
                /*
                 * Grammar production:
                 * f0 -> ClassDeclaration()
                 *       | ClassExtendsDeclaration()
                 */
                id = TypeCheckHelper.className(((TypeDeclaration) n).f0.choice);
            }

            if (declared.contains(id))
                return false;
            else
                declared.add(id);
        }

        return true;
    }

    public static MethodType methodType(MethodDeclaration m) {
        ArrayList<String> params = new ArrayList<>();

        /*
         * Grammar production:
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
        if (m.f4.present()) {
            /*
             * Grammar production:
             * f0 -> FormalParameter()
             * f1 -> ( FormalParameterRest() )*
             */
            FormalParameterList fpl = (FormalParameterList) m.f4.node;
            FormalParameter first = fpl.f0;
            NodeListInterface rest = fpl.f1;

            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             */
            params.add(TypeCheckHelper.makeExpressionType(first.f0).getType());
            for (Enumeration<Node> e = rest.elements(); e.hasMoreElements(); ) {
                /*
                 * Grammar production:
                 * f0 -> ","
                 * f1 -> FormalParameter()
                 */
                String type = TypeCheckHelper.makeExpressionType(((FormalParameterRest) e.nextElement()).f1.f0).getType();
                params.add(type);
            }
        }

        return new MethodType(params, TypeCheckHelper.makeExpressionType(m.f1));
    }

    public static Type extractTypeFromParamOrVar(Node n) {
        if (n instanceof FormalParameter) {
            /*
             * Grammar production:
             * f0 -> Type()
             * f1 -> Identifier()
             */
            return ((FormalParameter) n).f0;
        } else { // n instanceof VarDeclaration
            /*
             * f0 -> Type()
             * f1 -> Identifier()
             * f2 -> ";"
             */
            return ((VarDeclaration) n).f0;
        }
    }

    public static final String UNDEFINED = "";
    public static final String INT_ARRAY = "int[]";
    public static final String BOOLEAN = "boolean";
    public static final String INT = "int";

    public static ExpressionType makeExpressionType(Type t) {
        String retType;

        /*
         * Grammar production:
         * f0 -> ArrayType()
         *       | BooleanType()
         *       | IntegerType()
         *       | Identifier()
         */
        NodeChoice c = t.f0;
        if (c.which == 0)
            retType = TypeCheckHelper.INT_ARRAY;
        else if (c.which == 1)
            retType = TypeCheckHelper.BOOLEAN;
        else if (c.which == 2)
            retType = TypeCheckHelper.INT;
        else // c.which == 3
            retType = TypeCheckHelper.identifierName((Identifier) c.choice);

        return new ExpressionType(c.choice, retType);
    }

    public static boolean isBasicType(String type) {
        return type.equals(TypeCheckHelper.INT_ARRAY)
                || type.equals(TypeCheckHelper.BOOLEAN)
                || type.equals(TypeCheckHelper.INT);
    }
}
