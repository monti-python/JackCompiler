package test.java;

import main.java.Parser;
import main.java.Tokenizer;
import main.java.binding.TokenAdapter;
import main.java.binding.TokenList;
import main.java.exceptions.JackCompilerException;
import main.java.model.Token;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
    public void testParser() throws JackCompilerException, ParserConfigurationException, TransformerException, IOException, SAXException, XMLStreamException {
        System.out.println("Testing file: " + path.toString());
        Path ref_path = Paths.get(path.toString().replaceFirst("\\.jack$", ".xml"));
        List<Token> res_tokens = Tokenizer.tokenize(path);
        Parser parser = new Parser(res_tokens);
        Element element = parser.comp("class");

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.METHOD, "html");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));

        String raw_out = writer.getBuffer().toString().replaceAll("\\s", "");
        String raw_ref = new String(Files.readAllBytes(ref_path)).replaceAll("\\s", "");

        //System.out.println(raw_out);
        //System.out.println(raw_ref);
        Assert.assertEquals(raw_out, raw_ref);

    }
}
