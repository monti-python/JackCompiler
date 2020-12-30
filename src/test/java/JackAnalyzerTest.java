import binding.TokenAdapter;
import binding.TokenList;
import exceptions.JackCompilerException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import model.Token;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;


@RunWith(Parameterized.class)
public class JackAnalyzerTest {

    @Parameterized.Parameters
    public static Iterable<Path> data() throws IOException, URISyntaxException {
        // Find all .jack files in resources
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(".");
        assert url != null;
        return Files.find(
                Paths.get(url.toURI()),
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

    @Test
    public void testTokenize() throws JAXBException {
        System.out.println("Testing file: " + path.toString());
        JAXBContext jc = JAXBContext.newInstance(TokenList.class);
        Marshaller marshaller = jc.createMarshaller();
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setAdapter(new TokenAdapter());
        Path exp_xml_path = Paths.get(path.toString().replaceFirst("\\.jack$", "T.xml"));
        List<Token> exp_tokens = ((TokenList) unmarshaller.unmarshal(exp_xml_path.toFile())).getTokens();
        List<Token> res_tokens = Tokenizer.tokenize(path);

        assertArrayEquals(res_tokens.toArray(), exp_tokens.toArray());

    }
}
