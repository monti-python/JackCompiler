package test.java;

import main.java.Parser;
import main.java.Tokenizer;
import main.java.exceptions.JackCompilerException;
import main.java.model.Token;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


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
        String out = parser.compileClass();

        String raw_out = out.replaceAll("\\s", "");
        String raw_ref = new String(Files.readAllBytes(ref_path)).replaceAll("\\s", "");

        Assert.assertEquals(raw_out, raw_ref);
    }
}
