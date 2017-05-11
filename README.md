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
javac parser/*.java J2V.java 
java -cp "./;./parser/" J2V < java-file > vapor-file
```

To generate the `hw2.tgz` required by `Phase2Tester`:

```bash
mkdir hw2
cp -r codegen hw2/
cp -r typecheck hw2/
cp J2V.java hw2/
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
javac parser/\*.java J2V.java

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
    java -cp "./;./parser/" J2V < Phase2Tests/${t}.java > ${t}.vapor
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