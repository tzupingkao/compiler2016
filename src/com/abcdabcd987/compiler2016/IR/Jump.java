package com.abcdabcd987.compiler2016.IR;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by abcdabcd987 on 2016-04-08.
 */
public class Jump extends BranchInstruction {
    private BasicBlock target;

    public Jump(BasicBlock BB, BasicBlock target) {
        super(BB);
        this.target = target;
    }

    @Override
    public void accept(IIRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public VirtualRegister getDefinedRegister() {
        return null;
    }

    @Override
    public void setDefinedRegister(Register newReg) {
        assert false;
    }

    @Override
    public void setUsedRegister(Map<Register, Register> regMap) {
        assert false;
    }

    @Override
    public void renameDefinedRegister(Function<VirtualRegister, Integer> idSupplier) {
        assert false;
    }

    @Override
    public void renameUsedRegister(Function<VirtualRegister, Integer> idSupplier) {
        assert false;
    }

    public BasicBlock getTarget() {
        return target;
    }

    @Override
    public void insertSplitBlock(BasicBlock toBB, BasicBlock insertedBB) {
        if (target != toBB) return;
        target = insertedBB;
        updateConnectivity(curBB.getSucc(), toBB, insertedBB);
        updateConnectivity(toBB.getPred(), curBB, insertedBB);
    }
}
