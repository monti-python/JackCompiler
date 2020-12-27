package main.java;

import main.java.exceptions.JackCompilerException;
import main.java.model.Token;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


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

    private final Map<String, String> rules;
    private final TokenIterator tokenIterator;
    private final Document document;

    public Parser(List<Token> tokenList) throws ParserConfigurationException, IOException {
        Path grammarPath = FileSystems.getDefault().getPath("src/main/resources/jack.grammar");
        this.rules = Files.lines(grammarPath)
                .filter(s -> s.matches("^\\w+:.*"))
                .collect(Collectors.toMap(k -> k.split(":")[0].trim(), v -> v.split(":")[1].trim()));
        this.tokenIterator = new TokenIterator(tokenList);
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    public boolean test(String rule) {
        return testOffset(rule, 0);
    }

    public boolean testOffset(String rule, int offset) {
        Token head = tokenIterator.peek(offset);
        rule = rule.endsWith("^") ? rule.substring(0, rule.length()-1) : rule; // remove pass-through marker

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
        rule = rule.endsWith("^") ? rule.substring(0, rule.length()-1) : rule; // remove pass-through marker
        Token head = tokenIterator.curr();
        Element element;
        // TERMINAL RULE
        if ( (rule.startsWith("'") && rule.endsWith("'")) || (rule.startsWith("/") && rule.endsWith("/")) || (rule.startsWith("<") && rule.endsWith(">")) ) {
            if (!test(rule)) {
                throw new JackCompilerException(
                        "Expected token " + rule + " but found '" + head.getValue() + "'", head.getLine(), head.getColumn()
                );
            }
            String ruleType = head.getType().toString();
            String elementName = ruleType.substring(0, 1).toLowerCase() + ruleType.substring(1);  // decapitalize
            element = document.createElement(elementName);
            element.setTextContent(" "+head.getValue()+" ");
            tokenIterator.next();
        }
        // NON-TERMINAL RULE
        else if (this.rules.containsKey(rule)) {
            String[] childRules = this.rules.get(rule).split(" +");
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
            for (String rule_try : rule.substring(1, rule.length()-1).split("(?<!\\\\)\\|")) {
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

