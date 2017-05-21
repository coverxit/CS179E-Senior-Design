import syntaxtree.*;
import typecheck.*;
import java.util.*;

public class J2V {
    public static void main(String args[]) throws ParseException {
        new MiniJavaParser(System.in);
        Goal program = MiniJavaParser.Goal();
        Scope env = new Scope(program);

        program.accept(new FirstPhaseVisitor(), env);

	//making a list of vtables and records
	List<Vtable> VtableList = new ArrayList<Vtable>();
	List<Record> RecordList = new ArrayList<Record>();
	List<Symbol> classList = env.getAllSymbols();
	for(Symbol s : classList)
	{
		Vtable tempVtable = new Vtable(s.toString());
		Record tempRecord = new Record(s.toString());
		Binder classBinder = env.lookup(s);
		Scope methodScope = classBinder.getScope();
		List<Symbol> methodOrVar = methodScope.getAllSymbols();
		for(Symbol m : methodOrVar)
		{
			Binder methVarBinder = methodScope.lookup(m);
			Node checkType = methVarBinder.getType();
			String testVariable = "null";
			try
			{
				testVariable = Helper.methodName(checkType);
				tempVtable.addMethMem(m.toString(), 0);
			}
			catch (ClassCastException e)
			{
				testVariable = "null";
				tempRecord.addVarMem(m.toString(), 0);
			}
		}
		
		VtableList.add(tempVtable);
		//tempRecord.debugRecord();
		RecordList.add(tempRecord);
	}
	//populating vtable and records is done, NO CHANGES CAN ACCORD NOW 
	PrintHelper printer = new PrintHelper();
	Passer visitorPass = new Passer(printer, VtableList, RecordList);
	
	//printing vtables to out
	for(Vtable v: VtableList.subList(1,VtableList.size()))
	{
		visitorPass.getPrint().printVtable(v);
	}

	//traversing main code to translate
	program.accept(new CodeGenVisitor(), visitorPass);
    }
}
