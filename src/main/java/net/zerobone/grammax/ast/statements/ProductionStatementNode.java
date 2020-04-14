package net.zerobone.grammax.ast.statements;

import net.zerobone.grammax.GrammaxContext;
import net.zerobone.grammax.ast.entities.ProductionSymbol;

import java.util.LinkedList;

public class ProductionStatementNode extends StatementNode {

    public String nonTerminal;

    public LinkedList<ProductionSymbol> production;

    public String code;

    public ProductionStatementNode(String nonTerminal, LinkedList<ProductionSymbol> production, String code) {
        this.nonTerminal = nonTerminal;
        this.production = production;
        this.code = code;
    }

    @Override
    public void apply(GrammaxContext context) {
        context.addProduction(nonTerminal, this);
    }

}