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

public class Parser {

    private final HashMap<String, String> rules;
    private final Iterator<Token> tokenIterator;
    private Document document = null;
    private Token curToken = null;

    public Parser(Iterator<Token> tokenIterator) throws ParserConfigurationException {
        this.tokenIterator = tokenIterator;
        this.rules = new HashMap<>();
        rules.put("class", "'class' className '{' classVarDec* subRoutineDec* '}'");
        rules.put("classVarDec", "('static'|'field') type varName moreVars*^ ';'");
        rules.put("moreVars", "',' varName");
        rules.put("type", "('int'|'char'|'boolean'|className)");
        rules.put("className", "identifier");
        rules.put("varName", "identifier");
        rules.put("identifier", "/[_A-Za-z][_A-Za-z0-9]*/");
        rules.put("subRoutineDec", "('constructor'|'method'|'function') ('void'|type) subRoutineName '(' parameterList? ')' subroutineBody");
        rules.put("subRoutineName", "identifier");
        rules.put("parameterList", "type varName moreParameters*^");
        rules.put("moreParameters", "',' type varName");
        rules.put("subroutineBody", "'{' varDec* statement* '}'");
        rules.put("varDec", "'var' type varName moreVars*^ ';'");
        rules.put("statement", "(letStatement|returnStatement)");  // incomplete
        rules.put("letStatement", "'let' varName '=' expression ';'");
        rules.put("returnStatement", "'return' expression? ';'");
        rules.put("expression", "term opTerm*^");
        rules.put("opTerm", "op term");
        rules.put("term", "(integerConstant|varName)");  // incomplete
        rules.put("integerConstant", "/[0-9]+/");
        rules.put("op", "('+'|'-'|'*'|'/'|'&'|'<'|'>'|'=')");  // incomplete




        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        this.document = documentBuilder.newDocument();


    }

    Token advance() {
        return this.curToken != null ? this.curToken : (this.curToken = tokenIterator.next());
    }

    public boolean test(String rule) {
        String head = advance().getValue();
        if (rule.startsWith("'") && rule.endsWith("'")) {  // rule is a terminal rule
            return rule.substring(1, rule.length() - 1).equals(head);
        }
        else if (rule.startsWith("/") && rule.endsWith("/")) {  // rule is regex terminal rule
            return head.matches(rule.substring(1, rule.length()-1));
        }
        else if (this.rules.containsKey(rule)) {  // rule is non-terminal rule
            String first = this.rules.get(rule).split(" +")[0];
            return test(first);
        }
        else if (rule.endsWith("*") || rule.endsWith("?")) {  // rule is optional
            return true;
        }
        else if (rule.startsWith("(") && rule.endsWith(")")) {  // rule has different options
            for (String rule_try: rule.substring(1, rule.length()-1).split("\\|")) {
                if (test(rule_try)) {
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
        Token head = advance();
        Element element = null;
        // rule is a terminal rule
        if ( (rule.startsWith("'") && rule.endsWith("'")) || (rule.startsWith("/") && rule.endsWith("/")) ) {
            if (!test(rule)) {
                throw new JackCompilerException(
                        "Expected token " + rule + " but found '" + head.getValue() + "'", head.getLine(), head.getColumn()
                );
            }
            element = document.createElement(head.getType().toString().toLowerCase());
            element.setTextContent(" "+head.getValue()+" ");
            this.curToken = null;
        }
        // rule is non-terminal rule
        else if (this.rules.containsKey(rule)) {
            String[] childRules = this.rules.get(rule).split(" +");
            if (childRules.length == 1) return comp(childRules[0]);  // flatten nodes if only one descendant
            element = document.createElement(rule);
            for (String childRule: childRules) {
                if (childRule.endsWith("?")) {  // child rule is optional
                    String baseRule = childRule.substring(0, childRule.length()-1);
                    if (test(baseRule)) {
                        element.appendChild(comp(baseRule));
                    }
                }
                else if (childRule.endsWith("*")) {  // child rule is optional and may be repeated
                    String baseRule = childRule.substring(0, childRule.length()-1);
                    while (test(baseRule)) {
                        element.appendChild(comp(baseRule));
                    }
                }
                else if (childRule.endsWith("*^")) {
                    String baseRule = childRule.substring(0, childRule.length()-2);
                    while (test(baseRule)) {
                        NodeList children = comp(baseRule).getChildNodes();
                        while (children.getLength() != 0) {
                            element.appendChild(children.item(0));
                        }
                    }
                }
                else {
                    element.appendChild(comp(childRule));
                }
            }
        }

        else if (rule.startsWith("(") && rule.endsWith(")")) {  // rule has different options
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


    public void compileClass() throws JackCompilerException {
        compileLiteral("class");
        compileTokenType(Token.TokenType.Identifier);
        compileLiteral("{");
        while (Arrays.asList("static", "field").contains(advance().getValue())) {
            compileClassVarDec();
        }
        while (Arrays.asList("constructor", "function", "method").contains(advance().getValue())) {
            compileSubRoutineDec();
        }
        compileLiteral("}");
    }

    void compileClassVarDec() throws JackCompilerException {
        compilePattern("static|field");
        compileType();
        compileTokenType(Token.TokenType.Identifier);
        while (advance().getValue().equals(",")) {
            compileLiteral(",");
            compileTokenType(Token.TokenType.Identifier);
        }
        compileLiteral(";");
    }

    void compileSubRoutineDec() throws JackCompilerException {
        compilePattern("constructor|function|method");
        Token token = advance();
        if (!(Arrays.asList("int", "char", "boolean", "void").contains(token.getValue())) && !(token.getType() == Token.TokenType.Identifier)) {
            throw new JackCompilerException(
                    "Expected a valid return type but found '" + token.getValue() + "'", token.getLine(), token.getColumn()
            );
        }
        this.curToken = null;
        compileTokenType(Token.TokenType.Identifier);
        compileLiteral("(");
        if(!advance().getValue().equals(")")) compileParameterList();
        compileLiteral(")");

    }

    void compileParameterList() throws JackCompilerException {
        compileType();
        compileTokenType(Token.TokenType.Identifier);
        while (advance().getValue().equals(",")) {
            compileLiteral(",");
            compileType();
            compileTokenType(Token.TokenType.Identifier);
        }
    }

    void compileType() throws JackCompilerException {
        Token token = advance();
        if (!(Arrays.asList("int", "char", "boolean").contains(token.getValue())) && !(token.getType() == Token.TokenType.Identifier)) {
            throw new JackCompilerException(
                    "Expected a valid type but found '" + token.getValue() + "'", token.getLine(), token.getColumn()
            );
        }
        this.curToken = null;
    }

    private void compileLiteral(String value) throws JackCompilerException {
        Token token = advance();
        if (!token.getValue().equals(value)) {
            throw new JackCompilerException(
                    "Expected token " + value + " but found '" + token.getValue() + "'", token.getLine(), token.getColumn()
            );
//            return false;
        }
        this.curToken = null;
    }

    private void compilePattern(String regex) throws JackCompilerException {
        Token token = advance();
        if (!token.getValue().matches(regex)) {
            throw new JackCompilerException(
                    "Expected token " + regex + " but found '" + token.getValue() + "'", token.getLine(), token.getColumn()
            );
//            return false;
        }
        this.curToken = null;
    }

    private void compileTokenType(Token.TokenType tokenType) throws JackCompilerException {
        Token token = advance();
        if (!tokenType.equals(token.getType())) {
            throw new JackCompilerException(
                    "Expected token type '" + tokenType + "' but found type'" + token.getType() + "'", token.getLine(), token.getColumn()
            );
//            return false;
        }
        this.curToken = null;
    }
}
