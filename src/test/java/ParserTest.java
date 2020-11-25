package test.java;

import main.java.Parser;
import main.java.Tokenizer;
import main.java.binding.TokenAdapter;
import main.java.binding.TokenList;
import main.java.exceptions.JackCompilerException;
import main.java.model.Token;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class ParserTest {

    @Parameterized.Parameters
    public static Iterable<Path> data() throws IOException {
        // Find all .jack files
        return Files.find(
                Paths.get("src/test/resources"),
                3,
                (p, bfa) -> (bfa.isRegularFile() && p.toString().toLowerCase().endsWith(".jack"))
        ).collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public Path path;


    @Test
    public void testParser() throws JackCompilerException, ParserConfigurationException, TransformerException {
        //Path path = Paths.get("src/test/resources/TestParser.jck");
        System.out.println(path.toString());
        List<Token> res_tokens = Tokenizer.tokenize(path);
        Parser parser = new Parser(res_tokens);
        Element element = parser.comp("class");

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        String output = writer.getBuffer().toString();

        System.out.println(output);
    }
}
