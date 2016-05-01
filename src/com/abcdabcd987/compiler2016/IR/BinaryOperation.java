package com.abcdabcd987.compiler2016.IR;

import java.util.*;
import java.util.function.Function;

/**
 * Created by abcdabcd987 on 2016-04-07.
 */
public class BinaryOperation extends IRInstruction {
    public enum BinaryOp {
        ADD, SUB, MUL, DIV, MOD,
        SHL, SHR, AND, OR, XOR
    }

    private Register dest;
    private BinaryOp op;
    public IntValue lhs;
    private IntValue rhs;

    public BinaryOperation(BasicBlock BB, Register dest, BinaryOp op, IntValue lhs, IntValue rhs) {
        super(BB);
        this.dest = dest;
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        if (lhs instanceof Register) usedRegister.add((Register) lhs);
        if (rhs instanceof Register) usedRegister.add((Register) rhs);
    }

    public Register getDest() {
        return dest;
    }

    public BinaryOp getOp() {
        return op;
    }

    public IntValue getLhs() {
        return lhs;
    }

    public IntValue getRhs() {
        return rhs;
    }

    @Override
    public void accept(IIRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Register getDefinedRegister() {
        return dest;
    }

    @Override
    public void setDefinedRegister(Register newReg) {
        dest = newReg;
    }

    @Override
    public void setUsedRegister(Map<Register, Register> regMap) {
        if (lhs instanceof Register) lhs = regMap.get(lhs);
        if (rhs instanceof Register) rhs = regMap.get(rhs);
        updateUsedRegisterCollection(regMap);
    }

    @Override
    public void renameDefinedRegister(Function<VirtualRegister, Integer> idSupplier) {
        dest = ((VirtualRegister) dest).newSSARenamedRegister(idSupplier.apply((VirtualRegister) dest));
    }

    @Override
    public void renameUsedRegister(Function<VirtualRegister, Integer> idSupplier) {
        if (lhs instanceof VirtualRegister)
            lhs = ((VirtualRegister) lhs).newSSARenamedRegister(idSupplier.apply((VirtualRegister) lhs));
        if (rhs instanceof VirtualRegister)
            rhs = ((VirtualRegister) rhs).newSSARenamedRegister(idSupplier.apply((VirtualRegister) rhs));
    }

}
