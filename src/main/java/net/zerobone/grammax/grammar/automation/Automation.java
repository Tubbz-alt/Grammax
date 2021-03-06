package net.zerobone.grammax.grammar.automation;

import net.zerobone.grammax.grammar.Grammar;
import net.zerobone.grammax.grammar.Production;
import net.zerobone.grammax.grammar.ProductionSymbol;
import net.zerobone.grammax.grammar.Symbol;
import net.zerobone.grammax.grammar.automation.conflict.*;

import java.util.ArrayList;
import java.util.HashMap;

public final class Automation {

    public static final int ACTION_ACCEPT = -1;

    public final int stateCount;

    private final String[] nonTerminals;

    private final String[] terminals;

    public final AutomationProduction[] productions;

    private final HashMap<String, Integer> nonTerminalToIndex = new HashMap<>();

    private final HashMap<String, Integer> terminalToIndex = new HashMap<>();

    private final HashMap<Integer, Integer> productionIdToIndex = new HashMap<>();

    /**
     * Invariants:
     * 0 => empty (error transition)
     * > 0 => state id
     */
    public final int[] gotoTable;

    /**
     * Invariants:
     * 0 => empty (error transition)
     * -1 => accept
     * < -1 => reduce
     * > 0 => shift
     */
    public final int[] actionTable;

    private final String[] parserStateDescriptions;

    private final ArrayList<Conflict> conflicts = new ArrayList<>();

    public Automation(Grammar grammar, int stateCount) {

        // counts

        this.stateCount = stateCount;

        // - 1 because we will ignore the augmenting symbol
        int nonTerminalCount = grammar.getNonTerminalCount() - 1;

        // + 1 because of the eof token
        int terminalCount = grammar.getTerminalCount() + 1;

        // initialize non-terminals

        nonTerminals = new String[nonTerminalCount];

        int counter = 0;

        for (Symbol nonTerminal : grammar.getNonTerminalSymbols()) {

            if (nonTerminal == grammar.getStartSymbol()) {
                continue;
            }

            nonTerminals[counter] = nonTerminal.id;
            nonTerminalToIndex.put(nonTerminal.id, counter);
            counter++;

        }

        assert counter == nonTerminalCount;

        // initialize terminals

        terminals = new String[terminalCount];

        counter = 0;

        for (String terminal : grammar.getTerminals()) {

            terminals[counter] = terminal;
            terminalToIndex.put(terminal, counter);

            counter++;

        }

        terminals[counter] = Symbol.EOF.id;
        terminalToIndex.put(Symbol.EOF.id, counter);

        assert counter + 1 == terminalCount;

        // initialize productions

        productions = new AutomationProduction[grammar.getProductionCount() - 1];

        counter = 0;

        for (Production production : grammar) {

            if (production.getNonTerminal() == grammar.getStartSymbol()) {
                continue;
            }

            AutomationProduction convertedProduction = convertProduction(production);

            productions[counter] = convertedProduction;
            productionIdToIndex.put(production.getId(), counter);

            counter++;

        }

        // initialize tables

        actionTable = new int[terminalCount * stateCount];

        gotoTable = new int[nonTerminalCount * stateCount];

        // state descriptions

        parserStateDescriptions = new String[this.stateCount];

    }

    public int getTerminalCount() {
        return terminals.length;
    }

    public int getNonTerminalCount() {
        return nonTerminals.length;
    }

    public String nonTerminalToSymbol(int nonTerminalIndex) {
        return nonTerminals[nonTerminalIndex];
    }

    public String terminalToSymbol(int terminalIndex) {
        return terminals[terminalIndex];
    }

    public int symbolToNonTerminalIndex(String nonTerminal) {
        return nonTerminalToIndex.get(nonTerminal);
    }

    public int symbolToTerminalIndex(String terminal) {
        return terminalToIndex.get(terminal);
    }

    public int productionIdToIndex(int productionId) {
        return productionIdToIndex.get(productionId);
    }

    // helper methods

    private AutomationProduction convertProduction(Production production) {

        // production should not be the start symbol production

        AutomationProduction automationProduction = new AutomationProduction(
            symbolToNonTerminalIndex(production.getNonTerminal().id),
            production.code,
            production.body.size()
        );

        int i = 0;

        for (ProductionSymbol symbol : production.body) {

            automationProduction.body[i++] = new AutomationSymbol(
                symbol.symbol.isTerminal,
                symbol.symbol.isTerminal ? symbolToTerminalIndex(symbol.symbol.id) : symbolToNonTerminalIndex(symbol.symbol.id),
                symbol.argumentName
            );

        }

        return automationProduction;

    }

    private ConflictOption codeToConflictOption(int actionTableIndex) {

        int actionTableCode = actionTable[actionTableIndex];

        assert actionTableCode != 0;

        if (actionTableCode == ACTION_ACCEPT) {
            return new AcceptOption();
        }

        if (actionTableCode < 0) {
            // reduce
            return new ReduceOption(decodeProductionId(actionTableCode));
        }

        // code > 0
        // shift
        return new ShiftOption(actionTableIndex % getTerminalCount());

    }

    void writeShift(int state, Symbol terminal, int targetState) {

        assert targetState >= 0: "negative target state";
        assert terminal.isTerminal;

        int terminalIndex = symbolToTerminalIndex(terminal.id);

        if (actionTable[getTerminalCount() * state + terminalIndex] != 0) {

            conflicts.add(new Conflict(
                new ShiftOption(terminalIndex),
                codeToConflictOption(getTerminalCount() * state + terminalIndex),
                state
            ));

            return;
        }

        actionTable[getTerminalCount() * state + terminalIndex] = encodeTargetState(targetState);

    }

    void writeReduce(int state, Symbol terminal, int grammarProductionId) {

        int productionId = productionIdToIndex(grammarProductionId);

        int terminalIndex = symbolToTerminalIndex(terminal.id);

        if (actionTable[getTerminalCount() * state + terminalIndex] != 0) {

            conflicts.add(new Conflict(
                codeToConflictOption(getTerminalCount() * state + terminalIndex),
                new ReduceOption(productionId),
                state
            ));

            return;

        }

        actionTable[getTerminalCount() * state + terminalIndex] = encodeProductionId(productionId);

    }

    void writeAccept(int state) {

        int eofIndex = symbolToTerminalIndex(Symbol.EOF.id);

        if (actionTable[getTerminalCount() * state + eofIndex] != 0) {

            conflicts.add(new Conflict(
                new AcceptOption(),
                codeToConflictOption(getTerminalCount() * state + eofIndex),
                state
            ));

            return;

        }

        actionTable[getTerminalCount() * state + eofIndex] = ACTION_ACCEPT;

    }

    void writeGoto(int state, Symbol nonTerminal, int targetState) {

        assert targetState >= 0 : "invalid target state.";
        assert targetState != 0 : "there cannot be any transitions to zero state";

        int nonTerminalIndex = symbolToNonTerminalIndex(nonTerminal.id);

        assert gotoTable[getNonTerminalCount() * state + nonTerminalIndex] == 0;

        gotoTable[getNonTerminalCount() * state + nonTerminalIndex] = encodeTargetState(targetState);

    }

    public ArrayList<Conflict> getConflicts() {
        return conflicts;
    }

    public String getParsingStateDescription(int state) {
        assert state >= 0;
        return parserStateDescriptions[state];
    }

    void setParsingStateDescription(int state, String desc) {
        assert state >= 0;
        parserStateDescriptions[state] = desc;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("Non-terminal count: ");
        sb.append(getNonTerminalCount());
        sb.append('\n');

        sb.append("Terminal count: ");
        sb.append(getTerminalCount());
        sb.append('\n');

        sb.append("State count: ");
        sb.append(stateCount);
        sb.append(" (0 - ");
        sb.append(stateCount - 1);
        sb.append(")\n\n");

        // action table

        sb.append("Action table:\n");

        sb.append(String.format("%5s", "STATE"));

        // calculate cell width

        int actionCellWidth = 6;

        for (int t = 0; t < getTerminalCount(); t++) {

            int terminalLength = terminals[t].length();

            actionCellWidth = Math.max(actionCellWidth, terminalLength + 1);

        }

        String actionEmptyMarker;
        {
            StringBuilder markerBuilder = new StringBuilder(actionCellWidth - 1);
            for (int i = 1; i < actionCellWidth; i++) {
                markerBuilder.append('-');
            }
            actionEmptyMarker = markerBuilder.toString();
        }

        // write table header

        for (int t = 0; t < getTerminalCount(); t++) {

            sb.append(' ');
            sb.append('|');

            String terminal = terminals[t];

            sb.append(String.format("%"+actionCellWidth+"s", terminal));

        }

        sb.append('\n');

        for (int s = 0; s < stateCount; s++) {

            sb.append(String.format("%4d", s));

            sb.append(' ');

            for (int t = 0; t < getTerminalCount(); t++) {

                sb.append(' ');
                sb.append('|');

                int code = actionTable[s * getTerminalCount() + t];

                if (code == 0) {
                    sb.append(' ');
                    sb.append(actionEmptyMarker);
                    continue;
                }

                if (code == ACTION_ACCEPT) {
                    sb.append(String.format("%"+actionCellWidth+"s", "accept"));
                    continue;
                }

                if (code > 0) {
                    // shift
                    sb.append(String.format("%"+actionCellWidth+"s", "s" + decodeTargetState(code)));
                }
                else {
                    sb.append(String.format("%"+actionCellWidth+"s", "r" + decodeProductionId(code)));
                }

            }

            sb.append('\n');

        }

        // goto table

        sb.append("\n\nGoto table:\n");

        sb.append(String.format("%5s", "STATE"));

        // calculate cell width

        int gotoCellWidth = 6;

        for (int nt = 0; nt < getNonTerminalCount(); nt++) {

            int nonTerminalLength = nonTerminals[nt].length();

            gotoCellWidth = Math.max(gotoCellWidth, nonTerminalLength + 1);

        }

        String gotoEmptyMarker;
        {
            StringBuilder markerBuilder = new StringBuilder(gotoCellWidth - 1);
            for (int i = 1; i < gotoCellWidth; i++) {
                markerBuilder.append('-');
            }
            gotoEmptyMarker = markerBuilder.toString();
        }

        // write table header

        for (int nt = 0; nt < getNonTerminalCount(); nt++) {

            sb.append(" |");

            String nonTerminal = nonTerminals[nt];

            sb.append(String.format("%"+gotoCellWidth+"s", nonTerminal));

        }

        sb.append('\n');

        for (int s = 0; s < stateCount; s++) {

            sb.append(String.format("%4d", s));

            sb.append(' ');

            for (int t = 0; t < getNonTerminalCount(); t++) {

                sb.append(' ');
                sb.append('|');

                int entry = gotoTable[s * getNonTerminalCount() + t];

                if (entry == 0) {
                    sb.append(' ');
                    sb.append(gotoEmptyMarker);
                    continue;
                }

                sb.append(String.format("%"+gotoCellWidth+"s", decodeTargetState(entry)));

            }

            sb.append('\n');

        }

        // states

        sb.append('\n');

        for (int s = 0; s < stateCount; s++) {

            sb.append('\n');

            sb.append("State ");
            sb.append(s);
            sb.append(":\n");
            sb.append(getParsingStateDescription(s));

        }

        return sb.toString();

    }

    public static int encodeTargetState(int state) {
        return state + 1;
    }

    public static int decodeTargetState(int state) {
        return state - 1;
    }

    public static int encodeProductionId(int productionId) {
        return -productionId - 2;
    }

    public static int decodeProductionId(int code) {
        return encodeProductionId(code);
    }

}