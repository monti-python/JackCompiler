package main.java.binding;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.xml.internal.ws.util.StringUtils;
import main.java.model.Token;
import org.w3c.dom.*;

public class TokenAdapter extends XmlAdapter<Object, Token> {

    private final Document doc;

    public TokenAdapter() {
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Token unmarshal(Object rawElement) {
        Element element = (Element) rawElement;
        return new Token(
                Token.TokenType.valueOf(StringUtils.capitalize(element.getLocalName())),
                element.getTextContent().replaceAll("^ | $", "")
        );
    }

    @Override
    public Object marshal(Token token) {
        Element element = doc.createElement(StringUtils.decapitalize(token.getType().toString()));
        element.setTextContent(" " + token.getData() + " ");
        return element;
    }

}