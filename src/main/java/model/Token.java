package model;


public class Token {
    public enum TokenType {
        Comment("(((?s)/\\*\\*?.*?\\*/)|(//.*))"),
        Keyword("(class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return)"),
        Symbol("(\\{|\\}|\\(|\\)|\\[|\\]|\\.|,|;|\\+|-|\\*|/|&|\\||<|>|=|~)"),
        IntegerConstant("([0-9]+)"),
        StringConstant("((?<=\").*?(?=\"))"),
        Identifier("([_A-Za-z][_A-Za-z0-9]*)");

        public final String pattern;


        TokenType(String pattern) {
            this.pattern = pattern;
        }
    }

    private final TokenType type;
    private final String value;
    private final Integer line;
    private final Integer column;

    public Token(TokenType type, String value, Integer line, Integer col) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = col;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }


    public boolean equals(Object other) {
        if (this.getClass() != other.getClass()) return false;
        Token other_token = (Token)other;
        return this.getType() == other_token.getType() && this.getValue().equals(other_token.getValue());
    }

    @Override
    public String toString() {
        return String.format("(%s '%s')", type.name(), value);
    }
}