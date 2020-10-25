package test.java;

import main.java.binding.TokenList;
import main.java.model.Token;
import main.java.binding.TokenAdapter;
import main.java.Tokenizer;

import org.junit.Test;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

public class TokenizerTest {

    @Test
    public void testTokenize() throws IOException, JAXBException {
        // Find all .jack files
        List<Path> paths = Files.find(
                Paths.get("src/test/resources"),
                3,
                (p, bfa) -> (bfa.isRegularFile() && p.toString().toLowerCase().endsWith(".jack"))
        ).collect(Collectors.toList());
        // For each jack file, compare tokenized result with the reference xml (unmarshalled)
        for (Path path: paths) {
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

}
