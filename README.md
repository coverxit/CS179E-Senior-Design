# CS179E-Senior-Design

Link to project instructions: http://www.cs.ucr.edu/~lesani/teaching/cp/cp.html

## Phase 1: Type-checking

### Instructions

To compile and run manually:

```bash
javac parser/*.java typecheck/Typecheck.java
java -cp "./;./parser/;./typecheck/" Typecheck < java-file
```

To generate the `hw1.tgz` required by `Phase1Tester`:

```bash
mkdir hw1
cp -r typecheck hw1/
mv hw1/typecheck/Typecheck.java hw1/
tar zcf hw1.tgz hw1/
rm -rf hw1
```

### Test Result

```
===============
Deleting old output directory "./Output"...
Extracting files from "../hw1.tgz"...
Compiling program with 'javac'...
==== Running Tests ====
Basic-error [te]: pass
Basic [ok]: pass
BinaryTree-error [te]: pass
BinaryTree [ok]: pass
BubbleSort-error [te]: pass
BubbleSort [ok]: pass
Factorial-error [te]: pass
Factorial [ok]: pass
LinearSearch-error [te]: pass
LinearSearch [ok]: pass
LinkedList-error [te]: pass
LinkedList [ok]: pass
MoreThan4-error [te]: pass
MoreThan4 [ok]: pass
QuickSort-error [te]: pass
QuickSort [ok]: pass
TreeVisitor-error [te]: pass
TreeVisitor [ok]: pass
==== Results ====
- Valid Cases: 9/9
- Error Cases: 9/9
- Submission Size = 45 kB
```

## Phase 2: Intermediate Code Generation

### Instructions

To compile and run manually:

```bash
javac parser/*.java codegen/J2V.java 
java -cp "./;./parser/;./codegen/" J2V < java-file > vapor-file
```

To generate the `hw2.tgz` required by `Phase2Tester`:

```bash
mkdir hw2
cp -r codegen hw2/
cp -r typecheck hw2/
mv hw2/codegen/J2V.java hw2/
rm hw2/typecheck/Typecheck.java
tar zcf hw2.tgz hw2/
rm -rf hw2
```

### Test Result

```
===============
Deleting old output directory "./Output"...
Extracting files from "../hw2.tgz"...
Compiling program with 'javac'...
==== Running Tests ====
1-PrintLiteral: pass
2-Add: pass
3-Call: pass
4-Vars: pass
5-OutOfBounds: pass
BinaryTree: pass
BubbleSort: pass
Factorial: pass
LinearSearch: pass
LinkedList: pass
MoreThan4: pass
QuickSort: pass
TreeVisitor: pass
==== Results ====
Passed 13/13 test cases
- Submission Size = 79 kB
```

Shell script for checking if the generated vapor is identical to the provided one (in `Phase3Tests`):

```bash
#!/bin/sh

echo ===============
echo Compiling program with 'javac'...
javac parser/\*.java codegen/J2V.java 

echo ==== Running Tests ===
TESTS=(
    BinaryTree
    BubbleSort
    Factorial
    LinearSearch
    LinkedList
    MoreThan4
    QuickSort
    TreeVisitor
)

passCount=0
for t in ${TESTS[@]}; do
    java -cp "./;./parser/;./codegen/" J2V < Phase2Tests/${t}.java > ${t}.vapor
    diff --ignore-blank-lines --strip-trailing-cr ${t}.vapor Phase3Tests/${t}.vapor &>/dev/null
    if [ $? -eq 0 ]; then
        echo ${t}: pass
        (( passCount += 1 ))
    else
        echo ${t}: FAIL
    fi
done

echo ==== Results ====
echo Passed ${passCount}/${#TESTS[@]} test cases
```

Note that since there is no lower bounds checking of arrays in the provided vapor, 
we have to manually remove the corresponding lower bounds checking code (in `CodeGenHelper.boundsCheck`)
before running the above script.

The `diff` result between the `CodeGenHelper.boundsCheck` with lower bounds checking and the one without it:

```diff
--- CodeGenHelper-WithLowerBoundsCheck.java
+++ CodeGenHelper-WithoutLowerBoundCheck.java
 public static VariableLabel boundsCheck(VariableLabel l, VariableLabel ind, CodeGenPair p) {
     Translator t = p.getTranslator();
     LabelManager lm = t.getLabelManager();
     VariableLabel var = lm.newTempVariable();
-    JumpLabel jmp1 = lm.newBoundsJump();
-    JumpLabel jmp2 = lm.newBoundsJump();
+    JumpLabel jmp = lm.newBoundsJump();

     l = retrieveDerefOrFuncCall(l, p);
     t.outputAssignment(var, l.dereference());
-    t.outputAssignment(var, LtS(ind.toString(), var.toString()));
+    t.outputAssignment(var, Lt(ind.toString(), var.toString()));

-    t.outputIf(var, jmp1);
+    t.outputIf(var, jmp);
     t.getOutput().increaseIndent();
     t.outputError("array index out of bounds");
     t.getOutput().decreaseIndent();

-    t.outputJumpLabel(jmp1);
-    t.outputAssignment(var, LtS("-1", ind.toString()));
-    t.outputIf(var, jmp2);
-    t.getOutput().increaseIndent();
-    t.outputError("array index out of bounds");
-    t.getOutput().decreaseIndent();
-
-    t.outputJumpLabel(jmp2);
+    t.outputJumpLabel(jmp);
     t.outputAssignment(var, MulS(ind.toString(), "4"));
     t.outputAssignment(var, Add(var.toString(), l.toString()));

     // Return "[t.0+4]"
     return lm.localVariable(4, var.toString()).dereference();
 }
```

The output of the above script:

```
===============
Compiling program with javac...
==== Running Tests ===
BinaryTree: pass
BubbleSort: pass
Factorial: pass
LinearSearch: pass
LinkedList: pass
MoreThan4: pass
QuickSort: pass
TreeVisitor: pass
==== Results ====
Passed 8/8 test cases
```

## Phase 3: Register Allocation
### Instructions

To compile and run manually:

```bash
javac -cp "./;./parser/varpor-parser.jar" V2VM.java 
java -cp "./;./parser/vapor-parser.jar" V2VM < vapor-file > vaporM-file
```