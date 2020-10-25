package main.java;

import main.java.model.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    public static List<Token> tokenize(Path src_path) {
        List<Token> tokens = new ArrayList<>();
        try {
            // Read input file
            String content = new String(Files.readAllBytes(src_path));
            // Form full token pattern
            StringBuilder tokenPatternBuffer = new StringBuilder();
            for (Token.TokenType tokenType : Token.TokenType.values()) {
                tokenPatternBuffer.append(String.format("|(?<%s>%s)", tokenType.name(), tokenType.pattern));
            }
            Pattern tokenPattern = Pattern.compile(tokenPatternBuffer.substring(1));
            // Try each pattern for each token (double-loop)
            Matcher matcher = tokenPattern.matcher(content);
            while (matcher.find()) {
                for (Token.TokenType tokenType: Token.TokenType.values()) {
                    if (tokenType.equals(Token.TokenType.Comment)) continue;
                    String match = matcher.group(tokenType.name());
                    if (match != null) {
                        tokens.add(new Token(tokenType, match));
                        break;
                    }
                }
            }
            return tokens;
        }
        catch (IOException e) {
            System.out.println("Error while reading file: "+src_path);
            throw new RuntimeException(e);
        }
    }

}
