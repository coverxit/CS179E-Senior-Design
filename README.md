# CS179E-Senior-Design

Link to project instructions: http://www.cs.ucr.edu/~lesani/teaching/cp/cp.html

## Phase 1: Type-checking

### Instructions

To compile and run manually:

```
javac parser/*.java typecheck/Typecheck.java
java -cp ./:./parser/:./typecheck/ Typecheck < java-file
```

To generate the `hw1.tgz` required by `Phase1Tester`:

```
mkdir hw1
cp -r typecheck hw1/
cp hw1/typecheck/Typecheck.java hw1/
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

```
javac parser/*.java J2V.java 
java -cp ./:./parser/ J2V < java-file > vapor-file
```