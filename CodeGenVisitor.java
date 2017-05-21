import typecheck.*;
import syntaxtree.*;
import java.util.*;
import visitor.*;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order.  Your visitors may extend this class.
 */
public class CodeGenVisitor extends GJDepthFirst< PrintHelper , Passer > {
   
   /**
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
   public PrintHelper visit(MainClass n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      argu.pushClassStack(argu.getResult());
      argu.setCurrentRecord(argu.getResult());
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      argu.getPrint().printline("func Main()");
      n.f6.accept(this, argu);
      argu.pushClassStack(argu.getResult());
      argu.pushSymbolTable();
      argu.addSymbolTable("this", argu.getResult());
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      n.f13.accept(this, argu);
      n.f14.accept(this, argu);
      argu.upindent();
      n.f15.accept(this, argu);
      argu.getPrint().printline("ret");
      argu.popClassStack();
      argu.downindent();
      argu.clearTempStack();
      n.f16.accept(this, argu);
      n.f17.accept(this, argu);
      argu.popSymbolTable();
      argu.popClassStack();
      return _ret;
   }

   /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
   public PrintHelper visit(TypeDeclaration n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public PrintHelper visit(ClassDeclaration n, Passer argu) {
      PrintHelper _ret=null;
      argu.setCurrentClass(Helper.className(n));
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      argu.setCurrentRecord(argu.getResult());
      argu.pushClassStack(argu.getResult());
      argu.pushSymbolTable();
      argu.addSymbolTable("this", argu.getResult());
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      argu.popClassStack();
      argu.popSymbolTable();
      n.f5.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public PrintHelper visit(ClassExtendsDeclaration n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      n.f7.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public PrintHelper visit(VarDeclaration n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String tempType = argu.getResult();
      n.f1.accept(this, argu);
      String tempVar = argu.getResult();
      argu.addSymbolTable(tempVar, tempType);
      n.f2.accept(this, argu);
      return _ret;
   }

   /**
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
   public PrintHelper visit(MethodDeclaration n, Passer argu) {
      PrintHelper _ret=null;
      String currentClass = argu.getCurrentClass();
      String methodName = Helper.methodName(n);
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      argu.pushSymbolTable();
      argu.addSymbolTable("this", argu.getCurrentClass());
      n.f4.accept(this, argu);
      String params = argu.getParams();
      argu.clearParams();
      argu.getPrint().printline("func " + currentClass + "." + methodName + "(this" + params + ")");
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      argu.upindent();
      n.f7.accept(this, argu);
      n.f8.accept(this, argu);
      n.f9.accept(this, argu);
      n.f10.accept(this, argu);
      argu.getPrint().printline("ret " + argu.getResult());
      argu.popSymbolTable();
      n.f11.accept(this, argu);
      n.f12.accept(this, argu);
      argu.downindent();
      return _ret;
   }

   /**
    * f0 -> FormalParameter()
    * f1 -> ( FormalParameterRest() )*
    */
   public PrintHelper visit(FormalParameterList n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public PrintHelper visit(FormalParameter n, Passer argu) {
      PrintHelper _ret=null;
      argu.addParams(Helper.identifierName(n.f1));
      n.f0.accept(this, argu);
      String tempType = argu.getResult();
      n.f1.accept(this, argu);
      String tempVar = argu.getResult();
      argu.addSymbolTable(tempVar, tempType);
      return _ret;
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public PrintHelper visit(FormalParameterRest n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public PrintHelper visit(Type n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult(argu.getResult());
      return _ret;
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public PrintHelper visit(ArrayType n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      argu.setResult("int[]");
      return _ret;
   }

   /**
    * f0 -> "boolean"
    */
   public PrintHelper visit(BooleanType n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult("boolean");
      return _ret;
   }

   /**
    * f0 -> "int"
    */
   public PrintHelper visit(IntegerType n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult("int");
      return _ret;
   }

   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
   public PrintHelper visit(Statement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   public PrintHelper visit(Block n, Passer argu) {
      PrintHelper _ret=null;
      argu.upindent();
      argu.pushSymbolTable();
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      argu.downindent();
      argu.popSymbolTable();
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public PrintHelper visit(AssignmentStatement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String varName = argu.getResult();
      String checkVar = argu.getRecordName(varName);
      if(checkVar != "Null")
      {
         varName = checkVar;
      }
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String ExpResult = argu.getResult();
      n.f3.accept(this, argu);
      argu.getPrint().printline(varName + " = " + ExpResult);
      argu.addSymbolTable(varName, ExpResult);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public PrintHelper visit(ArrayAssignmentStatement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public PrintHelper visit(IfStatement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String tempVar = argu.getResult();
      String ifLabel = argu.newIfLabel();
      String ifendLabel = argu. newIfEndLabel();
      argu.getPrint().printline("if0 " + tempVar + " goto :" + ifLabel);
      argu.upindent();
      argu.pushSymbolTable();
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      argu.getPrint().printline("goto :" + ifendLabel);
      argu.downindent();
      argu.popSymbolTable();
      argu.getPrint().printline(ifLabel + ":");
      argu.upindent();
      argu.pushSymbolTable();
      n.f5.accept(this, argu);
      n.f6.accept(this, argu);
      argu.downindent();
      argu.popSymbolTable();
      argu.getPrint().printline(ifendLabel+ ":");
      return _ret;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public PrintHelper visit(WhileStatement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String tempVar = argu.getResult();
      String whilelabel = argu.newWhileLabel();
      String whileendlabel = argu.newWhileEndLabel();
      argu.getPrint().printline(whilelabel + ":");
      argu.getPrint().printline("if0 " + tempVar + " goto " + ":" + whileendlabel);
      argu.upindent();
      argu.pushSymbolTable();
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      argu.getPrint().printline("goto " + ":" + whilelabel);
      argu.downindent();
      argu.pushSymbolTable();
      argu.getPrint().printline(whileendlabel + ":");
      return _ret;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public PrintHelper visit(PrintStatement n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      argu.getPrint().printline("PrintIntS(" + argu.getResult() +")");
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */
   public PrintHelper visit(Expression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
   public PrintHelper visit(AndExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String left = argu.getResult();
      argu.setResult("");
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String right = argu.getResult();
      argu.setResult("");
      String tempVar = "t." + argu.newTempVariable();
      String Builtin = "Eq(" + left + " " + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public PrintHelper visit(CompareExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String left = argu.getResult();
      argu.setResult("");
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String right = argu.getResult();
      argu.setResult("");
      String tempVar = "t." + argu.newTempVariable();
      String Builtin = "LtS(" + left + " " + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public PrintHelper visit(PlusExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String left = argu.getResult();
      argu.setResult("");
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String right = argu.getResult();
      argu.setResult("");
      String tempVar = "t." + argu.newTempVariable();
      String Builtin = "Add(" + left + " " + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public PrintHelper visit(MinusExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String left = argu.getResult();
      argu.setResult("");
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String right = argu.getResult();
      argu.setResult("");
      String tempVar = "t." + argu.newTempVariable();
      String Builtin = "Sub(" + left + " " + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public PrintHelper visit(TimesExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      String left = argu.getResult();
      argu.setResult("");
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String right = argu.getResult();
      argu.setResult("");
      String tempVar = "t." + argu.newTempVariable();
      String Builtin = "Mul(" + left + " " + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public PrintHelper visit(ArrayLookup n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public PrintHelper visit(ArrayLength n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public PrintHelper visit(MessageSend n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      //Map<String, String> st = argu.peekSymbolTableStack();
      String tempVar = argu.getResult();
      argu.setResult("");
      boolean mismatchflag = false;
      if(tempVar != "this")
      {
         String idname = argu.getClassRep(tempVar);
         mismatchflag = true;
         String nlabel = argu.newNullLabel();
         argu.getPrint().printline("if " + tempVar + " goto :" + nlabel);
         argu.getPrint().printline("   Error(\"null pointer\")");
         argu.getPrint().printline(nlabel + ":");
         argu.pushClassStack(idname);
      }

      String newVar ="t." + argu.newTempVariable();
      String methodname = Helper.identifierName(n.f2);
      Vtable vt = argu.getVtable(argu.peekClassStack());
      String methodOffset = String.valueOf(vt.getMemSize(methodname));
      argu.getPrint().printline(newVar + " = [" + tempVar + "]");
      argu.getPrint().printline(newVar + " = [" + newVar + "+" + methodOffset + "]");
      if(mismatchflag)
      {
         argu.popClassStack();
      }
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      String arguments = argu.getArgumentString();
      argu.clearArgument();
      String newVar2 = "t." + argu.newTempVariable();
      argu.getPrint().printline(newVar2 + " = call " + newVar + "(" + tempVar + arguments + ")");
      argu.setResult(newVar2);
      n.f5.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Expression()
    * f1 -> ( ExpressionRest() )*
    */
   public PrintHelper visit(ExpressionList n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.addArgument(argu.getResult());
      n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public PrintHelper visit(ExpressionRest n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      argu.addArgument(argu.getResult());
      return _ret;
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
   public PrintHelper visit(PrimaryExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public PrintHelper visit(IntegerLiteral n, Passer argu) {
      PrintHelper _ret=null;
      argu.setResult(n.f0.toString());
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "true"
    */
   public PrintHelper visit(TrueLiteral n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult("1");
      return _ret;
   }

   /**
    * f0 -> "false"
    */
   public PrintHelper visit(FalseLiteral n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult("0");
      return _ret;
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public PrintHelper visit(Identifier n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      argu.setResult(Helper.identifierName(n));
      return _ret;
   }

   /**
    * f0 -> "this"
    */
   public PrintHelper visit(ThisExpression n, Passer argu) {
      PrintHelper _ret=null;
      argu.setResult("this");
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public PrintHelper visit(ArrayAllocationExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public PrintHelper visit(AllocationExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      n.f3.accept(this, argu);
      String tempVar = "t." + argu.newTempVariable();
      String idName = Helper.identifierName(n.f1);
      String allocamount = argu.getAllocAmount(idName);
      argu.getPrint().printline(tempVar + " = HeapAllocZ(" + allocamount + ")");
      argu.getPrint().printline("[" + tempVar + "]" + " = :" + "vmt_" +idName);
      argu.setResult(tempVar);
      argu.addSymbolTable(tempVar, idName);
      return _ret;
   }

   /**
    * f0 -> "!"
    * f1 -> Expression()
    */
   public PrintHelper visit(NotExpression n, Passer argu) {
      PrintHelper _ret=null;
      String tempVar = "t." + argu.newTempVariable();
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String right = argu.getResult();
      String Builtin = "Not(" + right + ")";
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public PrintHelper visit(BracketExpression n, Passer argu) {
      PrintHelper _ret=null;
      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      String tempVar = "t." + argu.newTempVariable();
      n.f2.accept(this, argu);
      String right = argu.getResult();
      String Builtin = right;
      String expline = tempVar + " = " + Builtin;
      argu.getPrint().printline(expline);
      argu.setResult(tempVar);
      return _ret;
   }

}
