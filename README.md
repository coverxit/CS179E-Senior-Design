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
javac -cp "./;./parser/varpor-parser.jar;./regalloc/" codegen/Output.java regalloc/V2VM.java 
java -cp "./;./parser/vapor-parser.jar;./regalloc/" V2VM < vapor-file > vaporM-file
```

To generate the `hw3.tgz` required by `Phase3Tester`:

```bash
mkdir -p hw3/codegen
cp codegen/Output.java hw3/codegen/
cp -r regalloc hw3/
mv hw3/regalloc/V2VM.java hw3/
tar zcf hw3.tgz hw3/
rm -rf hw3
```

### Test Result

Running `Phase3Tester` with all registers **(23)**:
	
- For global allocation: `$t0` ~ `$t8` and `$s0` ~ `$s7`.
- For local allocation (temporarily loading variables from stack): `$v0`, `$v1`, `$a0` ~ `$a3`.

```
===============
Deleting old output directory "./Output"...
Extracting files from "../hw3.tgz"...
Compiling program with 'javac'...
==== Running Tests ====
1-Basic: pass
2-Loop: pass
BinaryTree.opt: pass
BinaryTree: pass
BubbleSort.opt: pass
BubbleSort: pass
Factorial.opt: pass
Factorial: pass
LinearSearch.opt: pass
LinearSearch: pass
LinkedList.opt: pass
LinkedList: pass
MoreThan4.opt: pass
MoreThan4: pass
QuickSort.opt: pass
QuickSort: pass
TreeVisitor.opt: pass
TreeVisitor: pass
==== Results ====
Passed 18/18 test cases
- Submission Size = 33 kB
```

Corresponding factory methods in `regalloc.RegisterPool`:

```java
public class RegisterPool {
    /* ... */
    public static RegisterPool CreateGlobalPool() {
        Register[] regs = {
                Register.t0, Register.t1, Register.t2, Register.t3,
                Register.t4, Register.t5, Register.t6, Register.t7,
                Register.t8,
                Register.s0, Register.s1, Register.s2, Register.s3,
                Register.s4, Register.s5, Register.s6, Register.s7
        };

        return new RegisterPool(regs);
    }

    public static RegisterPool CreateLocalPool() {
        Register[] regs = {
                Register.v0, Register.v1,
                Register.a0, Register.a1, Register.a2, Register.a3
        };

        return new RegisterPool(regs);
    }
    /* ... */
}
```

In order to test whether we generated correct VaporM code when some variables are spilled onto the stack, we also tested running `Phase3Tester` with limited  registers **(6)**:

- For global allocation: `$t0` ~ `$t3`.
- For local allocation (temporarily loading variables from stack): `$v0`, `$v1`.

```
===============
Deleting old output directory "./Output"...
Extracting files from "../hw3.tgz"...
Compiling program with 'javac'...
==== Running Tests ====
1-Basic: pass
2-Loop: pass
BinaryTree.opt: pass
BinaryTree: pass
BubbleSort.opt: pass
BubbleSort: pass
Factorial.opt: pass
Factorial: pass
LinearSearch.opt: pass
LinearSearch: pass
LinkedList.opt: pass
LinkedList: pass
MoreThan4.opt: pass
MoreThan4: pass
QuickSort.opt: pass
QuickSort: pass
TreeVisitor.opt: pass
TreeVisitor: pass
==== Results ====
Passed 18/18 test cases
- Submission Size = 34 kB
```

Corresponding factory methods in `regalloc.RegisterPool`:

```java
public class RegisterPool {
    /* ... */
    public static RegisterPool CreateGlobalPool() {
        Register[] regs = {
                Register.t0, Register.t1, Register.t2, Register.t3
        };

        return new RegisterPool(regs);
    }

    public static RegisterPool CreateLocalPool() {
        Register[] regs = {
                Register.v0, Register.v1
        };

        return new RegisterPool(regs);
    }
    /* ... */
}
```

## Phase 4: Activation Records and Instruction Selection

### Instructions

To compile and run manually:

```bash
javac -cp "./;./parser/varpor-parser.jar" codegen/Output.java regalloc/Register.java VM2M.java 
java -cp "./;./parser/vapor-parser.jar" VM2M < vaporM-file > asm-file
```
To generate the `hw4.tgz` required by `Phase4Tester`:

```bash
mkdir hw4
mkdir hw4/codgen
mkdir hw4/regalloc
cp -r asmgen hw4/
cp codgen/Output.java hw4/codegen/
cp regalloc/Register.java hw4/regalloc/
cp VM2M.java hw4/
tar zcf hw4.tgz hw4/
rm -rf hw4
```

### Test Result

```
===============
Deleting old output directory "./Output"...
Extracting files from "../hw4.tgz"...
Compiling program with 'javac'...
==== Running Tests ====
BinaryTree.opt: pass
BinaryTree: pass
BubbleSort.opt: pass
BubbleSort: pass
Factorial.opt: pass
Factorial: pass
LinearSearch.opt: pass
LinearSearch: pass
LinkedList.opt: pass
LinkedList: pass
MoreThan4.opt: pass
MoreThan4: pass
QuickSort.opt: pass
QuickSort: pass
TreeVisitor.opt: pass
TreeVisitor: pass
==== Results ====
Passed 16/16 test cases
- Submission Size = 22 kB
```