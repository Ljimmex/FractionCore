package pl.Ljimmex.fractionCore.database.async;

/**
 * Runtime wrapper used to transport checked exceptions from database tasks
 * executed inside {@link DatabaseExecutor} back to the caller via
 * {@link java.util.concurrent.CompletableFuture}.
 */
public class DatabaseTaskException extends RuntimeException {

    public DatabaseTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
