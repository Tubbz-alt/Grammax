package net.zerobone.grammax.examples.calculator.tokens;

import net.zerobone.grammax.examples.calculator.parser.CalcParser;

public class NumberToken extends Token {

    public final double value;

    public NumberToken(int line, double value) {
        super(line, CalcParser.T_NUM);
        this.value = value;
    }

}