package main.java;

import main.java.exceptions.JackCompilerException;
import main.java.model.Token;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class TokenIterator {
    private final List<Token> tokenList;
    private int idx = 0;

    public TokenIterator(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    public Token curr() {
        return this.tokenList.get(idx);
    }

    public Token next() {
        return ++idx < tokenList.size() ? this.tokenList.get(idx) : null;
    }

    public Token peek(int offset) {
        return idx+offset < tokenList.size() ? this.tokenList.get(idx+offset): null;
    }

}


public class Parser {

    private final HashMap<String, String> rules;
    private final TokenIterator tokenIterator;
    private Document document = null;

    public Parser(List<Token> tokenList) throws ParserConfigurationException {
        this.tokenIterator = new TokenIterator(tokenList);
        this.rules = new HashMap<>();
        rules.put("class", "'class' <Identifier> '{' classVarDec* subroutineDec* '}'");
        rules.put("classVarDec", "('static'|'field') type <Identifier> moreVars*^ ';'");
        rules.put("moreVars", "',' <Identifier>");
        rules.put("type", "('int'|'char'|'boolean'|<Identifier>)");
        rules.put("subroutineDec", "('constructor'|'method'|'function') ('void'|type) <Identifier> '(' parameterList? ')' subroutineBody");
        rules.put("subroutineName", "<Identifier>");
        rules.put("parameterList", "type <Identifier> moreParameters*^");
        rules.put("moreParameters", "',' type <Identifier>");
        rules.put("subroutineBody", "'{' varDec* statements '}'");
        rules.put("statements", "statement*^");
        rules.put("varDec", "'var' type^ <Identifier> moreVars*^ ';'");
        rules.put("statement", "(letStatement|ifStatement|whileStatement|doStatement|returnStatement)");
        rules.put("letStatement", "'let' <Identifier> indexExpression?^ '=' expression ';'");
        rules.put("indexExpression", "'[' expression ']'");
        rules.put("ifStatement", "'if' '(' expression ')' '{' statement* '}' else?^");
        rules.put("else", "'else' '{' statement* '}'");
        rules.put("whileStatement", "'while' '(' expression ')' '{' statement* '}'");
        rules.put("doStatement", "'do' subroutineCall ';'");
        rules.put("returnStatement", "'return' expression? ';'");
        rules.put("subroutineCall", "(subroutineCall1|subroutineCall2)");
        rules.put("subroutineCall1", "<Identifier> '(' expressionList? ')'");
        rules.put("subroutineCall2", "<Identifier> '.' <Identifier> '(' expressionList? ')'");
        rules.put("expressionList", "expression moreExpressions*^");
        rules.put("moreExpressions", "',' expression");
        rules.put("expression", "term opTerm*^");
        rules.put("opTerm", "op term");
        rules.put("term", "(indexedExpression|subroutineCall|subExpression|unaryExpression|integerConstant|stringConstant|keywordConstant|<Identifier>)");  // incomplete
        rules.put("indexedExpression", "<Identifier> indexExpression");
        rules.put("subExpression", "'(' expression ')'");
        rules.put("unaryExpression", "unaryOp term");
        rules.put("integerConstant", "<IntegerConstant>");
        rules.put("stringConstant", "<StringConstant>");
        rules.put("keywordConstant", "('true'|'false'|'null'|'this')");
        rules.put("op", "('+'|'-'|'*'|'/'|'&'|'<'|'>'|'=')");  // incomplete
        rules.put("unaryOp", "('-'|'~')");




        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        this.document = documentBuilder.newDocument();


    }

    public boolean test(String rule) {
        return testOffset(rule, 0);
    }

    public boolean testOffset(String rule, int offset) {
        Token head = tokenIterator.peek(offset);
        // TERMINAL RULES
        if (rule.startsWith("'") && rule.endsWith("'")) {  // rule is a literal terminal rule
            return rule.substring(1, rule.length() - 1).equals(head.getValue());
        }
        else if (rule.startsWith("/") && rule.endsWith("/")) {  // rule is a regex-based terminal rule
            return head.getValue().matches(rule.substring(1, rule.length()-1));
        }
        else if (rule.startsWith("<") && rule.endsWith(">")) {  // rule is a type-based terminal rule
            return head.getType().toString().equals(rule.substring(1, rule.length()-1));
        }
        // NON-TERMINAL RULES
        else if (this.rules.containsKey(rule)) {  // rule is non-terminal rule
            String[] subrules = this.rules.get(rule).split(" +");
            String first = subrules[0];
            if (first.equals("<Identifier>") && subrules.length > 1) {
                // This is the only case when the Jack language is LL(2), must test first and second rules
                String second = subrules[1];
                return testOffset(first, offset) && testOffset(second, offset+1);
            }
            else {
                return testOffset(first, offset);
            }
        }
        else if (rule.endsWith("*") || rule.endsWith("?")) {  // rule is optional
            return true;
        }
        else if (rule.startsWith("(") && rule.endsWith(")")) {  // rule has different options
            for (String rule_try: rule.substring(1, rule.length()-1).split("\\|")) {
                if (testOffset(rule_try, offset)) {
                    return true;
                }
            }
            return false;
        }
        else {  // Unknown rule
            return false;
        }
    }

    public Element comp(String rule) throws JackCompilerException {
        Token head = tokenIterator.curr();
        Element element;
        // rule is a terminal rule
        if ( (rule.startsWith("'") && rule.endsWith("'")) || (rule.startsWith("/") && rule.endsWith("/")) || (rule.startsWith("<") && rule.endsWith(">")) ) {
            if (!test(rule)) {
                throw new JackCompilerException(
                        "Expected token " + rule + " but found '" + head.getValue() + "'", head.getLine(), head.getColumn()
                );
            }
            element = document.createElement(head.getType().toString().toLowerCase());
            element.setTextContent(" "+head.getValue()+" ");
            tokenIterator.next();
        }
        // rule is non-terminal rule
        else if (this.rules.containsKey(rule)) {
            String[] childRules = this.rules.get(rule).split(" +");
            //if (childRules.length == 1) return comp(childRules[0]);  // flatten nodes if only one descendant
            element = document.createElement(rule);
            for (String childRule: childRules) {
                boolean skip = false;
                List<Element> childrenElements = new ArrayList<>();
                // Check if we should pass children through to the parent element
                if (childRule.endsWith("^")) {
                    skip = true;
                    childRule = childRule.substring(0, childRule.length()-1);
                }
                // Gather children elements
                if (childRule.endsWith("?")) {  // child rule is optional
                    String baseRule = childRule.substring(0, childRule.length()-1);

                    if (test(baseRule)) {
                        childrenElements.add(comp(baseRule));
                    }
                }
                else if (childRule.endsWith("*")) {  // child rule is optional and may be repeated
                    String baseRule = childRule.substring(0, childRule.length()-1);
                    while (test(baseRule)) {
                        childrenElements.add(comp(baseRule));
                    }
                }
                else {
                    childrenElements.add(comp(childRule));
                }
                // Append the child element (skip=false) or pass-through all descendants directly (skip=true)
                for (Element childElement: childrenElements) {
                    if (skip) {
                        NodeList children = childElement.getChildNodes();
                        while (children.getLength() != 0) {
                            element.appendChild(children.item(0));
                        }
                    }
                    else {
                        element.appendChild(childElement);
                    }
                }
            }
        }
        // Rule has different options
        else if (rule.startsWith("(") && rule.endsWith(")")) {
            for (String rule_try : rule.substring(1, rule.length()-1).split("\\|")) {
                if (test(rule_try)) {
                    return comp(rule_try);
                }
            }
            throw new JackCompilerException(
                    "Expected token among " + rule + " but found '" + head.getValue() + "'", head.getLine(), head.getColumn()
            );
        }
        // Unknown rule
        else {
            throw new JackCompilerException(
                    "Unknown grammar rule: " + rule
            );
        }
        return element;
    }

}

