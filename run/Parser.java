/* This file was generated by Grammax v1.0.0-beta */

import java.util.Stack;

public final class Parser {
    public static final int T_MUL = 0;
    public static final int T_NUM = 1;
    public static final int T_LPAREN = 2;
    public static final int T_RPAREN = 3;
    public static final int T_PLUS = 4;
    public static final int T_EOF = 5;
    private static final int terminalCount = 6;
    private static final int nonTerminalCount = 3;
    private static final int[] gotoTable = {
        4,6,3,
        0,0,0,
        0,0,0,
        0,0,0,
        4,8,3,
        0,0,0,
        0,0,10,
        0,0,0,
        12,0,3,
        0,0,0,
        0,0,0,
        0,0,0};
    private static final int[] actionTable = {
        0,2,5,0,0,0,
        -7,0,0,-7,-7,-7,
        -5,0,0,-5,-5,-5,
        7,0,0,-3,-3,-3,
        0,2,5,0,0,0,
        0,0,0,0,9,-1,
        0,2,5,0,0,0,
        0,0,0,11,9,0,
        0,2,5,0,0,0,
        -4,0,0,-4,-4,-4,
        -6,0,0,-6,-6,-6,
        7,0,0,-2,-2,-2};
    private static final int[] productionLabels = {1,1,0,0,2,2};
    @SuppressWarnings("Convert2Lambda")
    private static final Reductor[] reductions = {
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                Object term = _grx_stack.pop().payload;
                _grx_stack.pop();
                Object expr = _grx_stack.pop().payload;
                Object v;
                {
                 v = (int)expr + (int)term; 
                }
                return v;
            }
        },
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                Object term = _grx_stack.pop().payload;
                Object v;
                {
                 v = term; 
                }
                return v;
            }
        },
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                Object factor = _grx_stack.pop().payload;
                _grx_stack.pop();
                Object term = _grx_stack.pop().payload;
                Object v;
                {
                 v = (int)term * (int)factor; 
                }
                return v;
            }
        },
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                Object factor = _grx_stack.pop().payload;
                Object v;
                {
                 v = factor; 
                }
                return v;
            }
        },
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                _grx_stack.pop();
                Object expr = _grx_stack.pop().payload;
                _grx_stack.pop();
                Object v;
                {
                 v = expr; 
                }
                return v;
            }
        },
        new Reductor() {
            @Override
            public Object reduce(Stack<StackEntry> _grx_stack) {
                Object n = _grx_stack.pop().payload;
                Object v;
                {
                 v = n; 
                }
                return v;
            }
        }
    };
    private static final StackEntry initialStackEntry = new StackEntry(0, null);
    private Stack<StackEntry> stack;
    private Object payload = null;
    private static final class StackEntry {
        private final int previousState;
        private final Object payload;
        private StackEntry(int previousState, Object payload) {
            this.previousState = previousState;
            this.payload = payload;
        }
    }
    private interface Reductor {
        Object reduce(Stack<StackEntry> _grx_stack);
    }
    public Parser() {
        stack = new Stack<>();
        stack.push(initialStackEntry);
    }
    public void reset() {
        stack.clear();
        stack.push(initialStackEntry);
        payload = null;
    }
    public void parse(int tokenId, Object tokenPayload) {
        while (true) {
            int action = actionTable[terminalCount * stack.peek().previousState + tokenId];
            if (action == 0) {
                throw new RuntimeException("Syntax error");
            }
            if (action == -1) {
                payload = stack.peek().payload;
                return;
            }
            if (action > 0) {
                stack.push(new StackEntry(action - 1, tokenPayload));
                return;
            }
            int productionIndex = -action - 2;
            Object reducedProduction = reductions[productionIndex].reduce(stack);
            StackEntry newState = stack.peek();
            int nextState = gotoTable[newState.previousState * nonTerminalCount + productionLabels[productionIndex]];
            stack.push(new StackEntry(nextState - 1, reducedProduction));
        }
    }
    public boolean successfullyParsed() {
        return payload != null;
    }
    public Object getValue() {
        assert payload != null : "parsing did not succeed";
        return payload;
    }
}