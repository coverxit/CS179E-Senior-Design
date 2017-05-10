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
        return new VariableLabel(varNo++);
    }

    public VariableLabel constant(String n) { return new VariableLabel(n); }

    public VariableLabel thisVariable() { return new VariableLabel("this"); }

    public VariableLabel classVariable(int offset) { return new VariableLabel(offset, "this"); }

    public VariableLabel localVariable(String n) { return new VariableLabel(n); }

    public VariableLabel localVariable(int offset, String n) { return new VariableLabel(offset, n); }

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
