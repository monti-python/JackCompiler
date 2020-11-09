package main.java;

import main.java.model.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class JackAnalyzer {

    public static void main(String[] args) throws IOException {

        for (String arg : args) {
            System.out.println(arg);
        }

        Path src_path = Paths.get(args[0]);
        Path tgt_path;

        //CodeWriter writer = new CodeWriter();

        List<Token> tokens = new ArrayList<>();

        if (src_path.toFile().isDirectory()) {
            List<Path> jackFiles = Files.list(src_path)
                    .filter(p -> p.toString().endsWith(".main.analyzer.jack"))
                    .collect(Collectors.toList());

            if (jackFiles.isEmpty()) {
                System.out.println("The source path ("+src_path+") doesn't contain any .main.analyzer.jack file");
                return;
            }
            tgt_path = src_path.resolve(src_path.getFileName()+".main.analyzer.jack");

//            tokens.addAll(writer.toAssembly(Collections.singletonList(new Command("call Sys.init 0"))));
//            for(Path jackFile: jackFiles) {
//               List<Command> commands = Cleaner.process(vmFile);
//               writer.setNamespace(vmFile.getFileName().toString());
//               List<String> instGroup = writer.toAssembly(commands);
//               tokens.addAll(instGroup);
//            }
        }
        else {
            if (!src_path.toString().trim().endsWith(".main.analyzer.jack")) {
                System.out.println("The source file ("+src_path+") doesn't correspond to a .main.analyzer.jack file");
                return;
            }
            else if(!src_path.toFile().exists()) {
                System.out.println("The source file ("+src_path+") doesn't exist");
                return;
            }
//            tgt_path = src_path.resolveSibling(src_path.getParent().getFileName()+".vm");
            tokens = Tokenizer.tokenize(src_path);
        }


        System.out.println(tokens);
        //Files.write(tgt_path, instructions);

    }
}
