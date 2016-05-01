package com.abcdabcd987.compiler2016.MIPS;

import com.abcdabcd987.compiler2016.CompilerOptions;
import com.abcdabcd987.compiler2016.IR.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static com.abcdabcd987.compiler2016.MIPS.MIPSRegisterSet.*;

/**
 * Created by abcdabcd987 on 2016-05-01.
 */
public class TargetIRTransformer {
    private static class FunctionInfo {
        int beginArg;
        int beginSavedReg;
        int beginRA;
        int beginLocal;
        int beginTempReg;
        int frameSize;
        List<PhysicalRegister> usedCallerSaveRegister;
        List<PhysicalRegister> usedCalleeSaveRegister;
        Map<StackSlot, Integer> stackSlotOffset = new HashMap<>();
    }

    private IRRoot irRoot;
    private final int sizeWord = CompilerOptions.getSizeInt();
    private Map<Function, FunctionInfo> funcInfo = new HashMap<>();

    public TargetIRTransformer(IRRoot irRoot) {
        this.irRoot = irRoot;
    }

    /**
     * <pre>
     *     | previous frame |
     *     |________________|    <-  $sp + frameSize           (high)
     *     | $t? backup     |                                    ^
     *     | ...            |                                    |
     *     | $t0 backup     |    <- $sp + beginTempReg           |
     *     |----------------|                                    |
     *     | local data m-1 |                                    |
     *     | ...            |                                    |
     *     | local data 0   |    <- $sp + beginLocal             |
     *     |----------------|                                    |
     *     | return address |    <- $sp + beginRA                |
     *     |----------------|                                    |
     *     | $s? backup     |                                    |
     *     | ...            |                                    |
     *     | $s0 backup     |    <- $sp + beginSavedReg          |
     *     |----------------|                                    |
     *     | arg n-1        |                                    |
     *     | ...            |                                    |
     *     | arg 0          |    <- $sp + beginArg             (low)
     *     |----------------|
     * </pre>
     */
    private void calcFrame(Function func) {
        FunctionInfo info = funcInfo.get(func);
        info.usedCallerSaveRegister = func.usedPhysicalGeneralRegister.stream().filter(PhysicalRegister::isCallerSave).collect(Collectors.toList());
        info.usedCalleeSaveRegister = func.usedPhysicalGeneralRegister.stream().filter(PhysicalRegister::isCalleeSave).collect(Collectors.toList());

        info.beginArg = 0;
        info.beginSavedReg = info.beginArg      + func.argVarRegList.size()          * sizeWord;
        info.beginRA       = info.beginSavedReg + info.usedCalleeSaveRegister.size() * sizeWord;
        info.beginLocal    = info.beginRA       +                                      sizeWord;
        info.beginTempReg  = info.beginLocal    + func.stackSlots.size()             * sizeWord;
        info.frameSize     = info.beginTempReg  + info.usedCallerSaveRegister.size() * sizeWord;

        for (int i = 0; i < func.argVarRegList.size(); ++i) {
            Register arg = func.argVarRegList.get(i);
            if (arg instanceof StackSlot) {
                StackSlot slot = (StackSlot) arg;
                info.stackSlotOffset.put(slot, info.beginArg + i * sizeWord);
            }
        }

        for (int i = 0; i < func.stackSlots.size(); ++i) {
            StackSlot slot = func.stackSlots.get(i);
            info.stackSlotOffset.put(slot, info.beginLocal + i* sizeWord);
        }
    }

    private void modifyEntry(Function func) {
        FunctionInfo info = funcInfo.get(func);
        BasicBlock entryBB = func.getStartBB();
        IRInstruction firstInst = func.getStartBB().getHead();

        // extend frame
        firstInst.prepend(new BinaryOperation(entryBB, SP, BinaryOperation.BinaryOp.SUB, SP, new IntImmediate(info.frameSize)));

        // save $ra
        firstInst.prepend(new Store(entryBB, sizeWord, SP, info.beginRA, RA));

        // save $s?
        for (int i = 0; i < info.usedCalleeSaveRegister.size(); ++i)
            firstInst.prepend(new Store(entryBB, sizeWord, SP, info.beginSavedReg + i * sizeWord, info.usedCalleeSaveRegister.get(i)));
    }

    private void modifyReturn(Function func) {
        // move to $v0 on return instruction
        for (Return ret : func.retInstruction) {
            ret.prepend(new Move(ret.getBasicBlock(), V0, ret.getRet()));
        }

        // if multiple return instruction, merge to an exit block
        if (func.retInstruction.size() > 1) {
            BasicBlock exitBB = new BasicBlock(func, "exit");
            exitBB.append(new Return(exitBB, V0));
            for (Return ret : func.retInstruction) {
                ret.prepend(new Jump(ret.getBasicBlock(), exitBB));
                ret.remove();
            }
            func.exitBB = exitBB;
        } else {
            func.exitBB = func.retInstruction.get(0).getBasicBlock();
        }
    }

    private void modifyExit(Function func) {
        FunctionInfo info = funcInfo.get(func);
        BasicBlock exitBB = func.exitBB;
        IRInstruction lastInst = exitBB.getLast();

        // restore $s?
        for (int i = 0; i < info.usedCalleeSaveRegister.size(); ++i)
            lastInst.prepend(new Load(exitBB, info.usedCalleeSaveRegister.get(i), sizeWord, SP, info.beginSavedReg + i * sizeWord));

        // restore $ra
        lastInst.prepend(new Load(exitBB, RA, sizeWord, SP, info.beginRA));

        // shrink frame
        lastInst.prepend(new BinaryOperation(exitBB, SP, BinaryOperation.BinaryOp.ADD, SP, new IntImmediate(info.frameSize)));
    }

    private void modifyStackSlot(Function func, FunctionInfo info, BasicBlock BB, IRInstruction inst) {
        // stack slot -> $sp offset
        if (inst instanceof Load) {
            Load load = (Load) inst;
            if (load.address instanceof StackSlot) {
                load.address = SP;
                load.offset = info.stackSlotOffset.get(load.address);
            }
        } else if (inst instanceof Store) {
            Store store = (Store) inst;
            if (store.address instanceof StackSlot) {
                store.address = SP;
                store.offset = info.stackSlotOffset.get(store.address);
            }
        }
    }

    private void modifyCall(Function func, FunctionInfo info, BasicBlock BB, IRInstruction inst) {
        if (!(inst instanceof Call)) return;
        Call call = (Call) inst;
        Function callee = call.getFunc();
        FunctionInfo calleeInfo = funcInfo.get(callee);
        List<IntValue> args = call.getArgs();

        // save $t? register
        for (int i = 0; i < info.usedCallerSaveRegister.size(); ++i)
            inst.prepend(new Store(BB, sizeWord, SP, info.beginTempReg + i * sizeWord, info.usedCallerSaveRegister.get(i)));

        // save $a? register
        if (func.argVarRegList.size() > 0) inst.prepend(new Store(BB, sizeWord, SP, info.beginArg, A0));
        if (func.argVarRegList.size() > 1) inst.prepend(new Store(BB, sizeWord, SP, info.beginArg + sizeWord, A1));
        if (func.argVarRegList.size() > 2) inst.prepend(new Store(BB, sizeWord, SP, info.beginArg + 2*sizeWord, A2));
        if (func.argVarRegList.size() > 3) inst.prepend(new Store(BB, sizeWord, SP, info.beginArg + 3*sizeWord, A3));

        // copy argument
        if (args.size() > 0) inst.prepend(new Move(BB, A0, args.get(0)));
        if (args.size() > 1) inst.prepend(new Move(BB, A1, args.get(1)));
        if (args.size() > 2) inst.prepend(new Move(BB, A2, args.get(2)));
        if (args.size() > 3) inst.prepend(new Move(BB, A3, args.get(3)));
        for (int i = 0; i < args.size(); ++i)
            inst.prepend(new Store(BB, sizeWord, SP, -calleeInfo.frameSize + calleeInfo.beginArg + i * sizeWord, args.get(i)));

        // restore $a? register
        if (func.argVarRegList.size() > 0) inst.append(new Load(BB, A0, sizeWord, SP, info.beginArg));
        if (func.argVarRegList.size() > 1) inst.append(new Load(BB, A1, sizeWord, SP, info.beginArg + sizeWord));
        if (func.argVarRegList.size() > 2) inst.append(new Load(BB, A2, sizeWord, SP, info.beginArg + 2*sizeWord));
        if (func.argVarRegList.size() > 3) inst.append(new Load(BB, A3, sizeWord, SP, info.beginArg + 3*sizeWord));

        // restore $t? register
        for (int i = 0; i < info.usedCallerSaveRegister.size(); ++i)
            inst.append(new Load(BB, info.usedCallerSaveRegister.get(i), sizeWord, SP, info.beginTempReg + i * sizeWord));

        // move result
        if (call.getDest() != null) inst.append(new Move(BB, call.getDest(), V0));
    }

    public void run() {
        for (Function func : irRoot.functions.values()) {
            FunctionInfo info = new FunctionInfo();
            funcInfo.put(func, info);
            calcFrame(func);
        }

        for (Function func : irRoot.functions.values()) {
            FunctionInfo info = funcInfo.get(func);
            modifyEntry(func);
            modifyReturn(func);
            modifyExit(func);

            for (BasicBlock BB : func.getReversePostOrder()) {
                for (IRInstruction inst = BB.getHead(); inst != null; inst = inst.getNext()) {
                    modifyCall(func, info, BB, inst);
                    modifyStackSlot(func, info, BB, inst);
                }
            }
        }
    }
}
