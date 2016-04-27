package com.abcdabcd987.compiler2016.IR;

import java.util.*;

/**
 * Created by abcdabcd987 on 2016-04-07.
 */
public class BasicBlock {
    private IRInstruction head = null;
    private IRInstruction last = null;
    private boolean ended = false;
    private String hintName;
    public Map<VirtualRegister, PhiInstruction> phi = new HashMap<>();

    //==== graph information

    /**
     * control flow graph predecessor set
     */
    private Set<BasicBlock> pred = new HashSet<>();

    /**
     * control flow graph successor set
     */
    private Set<BasicBlock> succ = new HashSet<>();

    /**
     * dominance frontier set
     */
    public Set<BasicBlock> DF;

    /**
     * immediate dominator
     */
    public BasicBlock IDom;

    /**
     * children in the dominance tree
     */
    public Set<BasicBlock> DTChildren;

    /**
     * number in the post order traverse of control flow graph
     */
    public int postOrderNumber;

    public BasicBlock(String hintName) {
        this.hintName = hintName != null ? hintName : "block";
    }

    public boolean isEnded() {
        return ended;
    }

    public void append(IRInstruction next) {
        if (ended) {
            throw new RuntimeException("Cannot append instruction after a basic block ends.");
        }
        if (last != null) {
            last.linkNext(next);
            last = next;
        } else {
            head = last = next;
        }
    }

    public void appendBeforeJump(IRInstruction next) {
        if (!ended) {
            throw new RuntimeException("The block has not been finished yet.");
        }
        if (last.getPrev() == null) {
            head = next;
        } else {
            last.getPrev().linkNext(next);
        }
        next.linkNext(last);
    }

    private void addSucc(BasicBlock BB) {
        if (BB == null) return;
        succ.add(BB);
        BB.pred.add(this);
    }

    public void end(BranchInstruction next) {
        append(next);
        ended = true;
        if (next instanceof Branch) {
            addSucc(((Branch) next).getThen());
            addSucc(((Branch) next).getElse());
        } else if (next instanceof Jump) {
            addSucc(((Jump) next).getTarget());
        }
    }

    public void setHead(IRInstruction head) {
        this.head = head;
    }

    public void setLast(IRInstruction last) {
        this.last = last;
    }

    public IRInstruction getHead() {
        return head;
    }

    public IRInstruction getLast() {
        return last;
    }

    public void accept(IIRVisitor visitor) {
        visitor.visit(this);
    }

    public String getHintName() {
        return hintName;
    }

    public Set<BasicBlock> getPred() {
        return pred;
    }

    public Set<BasicBlock> getSucc() {
        return succ;
    }

    @Override
    public String toString() {
        return hintName;
    }
}