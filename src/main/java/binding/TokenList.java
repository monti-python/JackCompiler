package main.java.binding;

import main.java.model.Token;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;


@XmlRootElement(name = "tokens")
@XmlAccessorType(XmlAccessType.FIELD)
public class TokenList {
    @XmlAnyElement
    @XmlJavaTypeAdapter(TokenAdapter.class)
    private List<Token> tokens;

    public TokenList() {}
    public TokenList(List<Token> tokenList) {
        this.tokens = tokenList;
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
