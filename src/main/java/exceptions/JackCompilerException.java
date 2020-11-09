package main.java.exceptions;

public class JackCompilerException extends Exception {

    public JackCompilerException(String message) {
        this(message, null, null);
    }

    public JackCompilerException(String message, Integer lineNumber, Integer colNumber) {
        super(message + " [line: "+lineNumber+", col: "+colNumber+"]");
    }
}
