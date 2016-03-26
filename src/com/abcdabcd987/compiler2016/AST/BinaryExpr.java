package com.abcdabcd987.compiler2016.AST;

/**
 * Created by abcdabcd987 on 2016-03-26.
 */
public class BinaryExpr extends Expr {
    public enum BinaryOP {
        ASSIGN,
        LOGICAL_OR, LOGICAL_AND,
        BITWISE_OR, BITWISE_AND, XOR,
        EQ, NE, LT, GT, LE, GE,
        SHL, SHR,
        ADD, SUB,
        MUL, DIV, MOD
    }

    public final BinaryOP op;
    public final Expr lhs;
    public final Expr rhs;

    public BinaryExpr(BinaryOP op, Expr lhs, Expr rhs) {
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }
}
