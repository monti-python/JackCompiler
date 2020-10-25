package main.java.model;


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
    private final String data;

    public Token(TokenType type, String data) {
        this.type = type;
        this.data = data;
    }

    public TokenType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public boolean equals(Object other) {
        if (this.getClass() != other.getClass()) return false;
        Token other_token = (Token)other;
        return this.getType() == other_token.getType() && this.getData().equals(other_token.getData());
    }

    @Override
    public String toString() {
        return String.format("(%s '%s')", type.name(), data);
    }
}