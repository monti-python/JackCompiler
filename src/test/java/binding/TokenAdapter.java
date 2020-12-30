package binding;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.xml.ws.util.StringUtils;
import model.Token;
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
                element.getTextContent().replaceAll("^ | $", ""),
                null,
                null
        );
    }

    @Override
    public Object marshal(Token token) {
        Element element = doc.createElement(StringUtils.decapitalize(token.getType().toString()));
        element.setTextContent(" " + token.getValue() + " ");
        return element;
    }

}