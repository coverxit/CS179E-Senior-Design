package codegen;

public class LabelManager {
    // jumpNo is counted globally (start from 1)
    private int nullJmpNo = 1;
    private int boundsJmpNo = 1;
    private int ifJmpNo = 1;
    private int whileJumpNo = 1;
    private int andJumpNo = 1;

    // varNo is counted locally (start from 0)
    private int varNo = 0;

    public void beginMethod() {
        varNo = 0;
    }

    public void endMethod() {
        // do nothing, just for pair.
    }

    public VariableLabel newTempVariable() {
        return VariableLabel.TempVariable(varNo++);
    }

    public VariableLabel functionCall(String fc) {
        return VariableLabel.FunctionCall(fc);
    }

    public VariableLabel constant(String c) {
        return VariableLabel.Constant(c);
    }

    public VariableLabel thisVariable() {
        return VariableLabel.This();
    }

    public VariableLabel thisVariable(int offset) {
        return VariableLabel.This(offset);
    }

    public VariableLabel localVariable(String var) {
        return VariableLabel.Local(var);
    }

    public VariableLabel localVariable(int offset, String var) {
        return VariableLabel.Local(offset, var);
    }

    public JumpLabel newNullJump() {
        return JumpLabel.Null(nullJmpNo++);
    }

    public JumpLabel newBoundsJump() {
        return JumpLabel.Bounds(boundsJmpNo++);
    }

    // Call newIfEndJump before newIfElseJump to generate the new label!
    public JumpLabel newIfElseJump() {
        return JumpLabel.IfElse(ifJmpNo);
    }

    public JumpLabel newIfEndJump() {
        return JumpLabel.IfEnd(ifJmpNo++);
    }

    // Call newWhileEndJump before newWhileTopJump to generate the new label!
    public JumpLabel newWhileTopJump() {
        return JumpLabel.WhileTop(whileJumpNo);
    }

    public JumpLabel newWhileEndJump() {
        return JumpLabel.WhileEnd(whileJumpNo++);
    }

    // Call newAndEndJump before newAndElseJump to generate the new label!
    public JumpLabel newAndElseJump() {
        return JumpLabel.AndElse(andJumpNo);
    }

    public JumpLabel newAndEndJump() {
        return JumpLabel.AndEnd(andJumpNo++);
    }
}
