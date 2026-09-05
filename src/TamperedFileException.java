public class TamperedFileException extends Exception {
    public TamperedFileException(String message) {
        super(message);
    }

    public TamperedFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
