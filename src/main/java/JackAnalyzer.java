
import exceptions.JackCompilerException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class JackAnalyzer {

    public static void main(String[] args) throws IOException, ParserConfigurationException, JackCompilerException, TransformerException {

        for (String arg : args) {
            System.out.println(arg);
        }

        Path src_path = Paths.get(args[0]);
        Path tgt_path;
        List<Path> jackFiles;

        if(!src_path.toFile().exists()) {
            System.out.println("The path ("+src_path+") doesn't exist");
            return;
        }

        if (src_path.toFile().isDirectory()) {
            jackFiles = Files.list(src_path)
                    .filter(p -> p.toString().endsWith(".jack"))
                    .collect(Collectors.toList());

            if (jackFiles.isEmpty()) {
                System.out.println("The source path ("+src_path+") doesn't contain any .jack file");
                return;
            }
        }
        else {
            if (!src_path.toString().trim().endsWith(".jack")) {
                System.out.println("The source file ("+src_path+") doesn't correspond to a .jack file");
                return;
            }
            jackFiles = Collections.singletonList(src_path);
        }

        for(Path jackFile: jackFiles) {
            tgt_path = Paths.get(jackFile.toString().replaceFirst("\\.jack$", ".xml"));
            System.out.println("Saving to file: " + tgt_path);
            String out = new Parser(Tokenizer.tokenize(jackFile)).compileClass();
            Files.write(tgt_path, out.getBytes());
        }

    }
}
