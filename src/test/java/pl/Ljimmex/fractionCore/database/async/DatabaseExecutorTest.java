package pl.Ljimmex.fractionCore.database.async;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseExecutorTest {

    private DatabaseExecutor executor;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        executor = new DatabaseExecutor(plugin, Executors.newSingleThreadExecutor());
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void supplyAsyncReturnsValue() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = executor.supplyAsync(() -> "ok");
        assertEquals("ok", future.get());
    }

    @Test
    void supplyAsyncWrapsCheckedException() {
        CompletableFuture<String> future = executor.supplyAsync(() -> {
            throw new RuntimeException("boom");
        });
        try {
            future.get();
        } catch (ExecutionException e) {
            assertInstanceOf(DatabaseTaskException.class, e.getCause());
            assertEquals("boom", e.getCause().getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void runAsyncExecutesTask() throws ExecutionException, InterruptedException {
        AtomicReference<String> ref = new AtomicReference<>();
        CompletableFuture<Void> future = executor.runAsync(() -> ref.set("done"));
        future.get();
        assertEquals("done", ref.get());
    }

    @Test
    void shutdownStopsExecutor() {
        executor.shutdown();
        assertTrue(executor.isShutdown());
    }
}
