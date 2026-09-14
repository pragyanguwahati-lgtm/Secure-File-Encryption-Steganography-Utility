/**
 * Root-level entry point wrapper forwarding to com.stego.cli.MainCLI.
 * Maintained for backwards-compatibility with direct 'java MainCLI' invocations.
 */
public class MainCLI {
    public static void main(String[] args) {
        com.stego.cli.MainCLI.main(args);
    }
}
