package pl.Ljimmex.fractionCore.database.async;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor for asynchronous database operations.
 * <p>
 * All blocking database calls should be submitted through this executor
 * to avoid freezing the server main thread. Results are returned as
 * {@link CompletableFuture} and can be synchronized back to the main thread
 * via {@link #sync(CompletableFuture)}.
 */
public final class DatabaseExecutor {

    private final ExecutorService executor;
    private final JavaPlugin plugin;
    private final Logger logger;

    public DatabaseExecutor(JavaPlugin plugin) {
        this(plugin, Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "fractioncore-db");
            t.setDaemon(true);
            return t;
        }));
    }

    public DatabaseExecutor(JavaPlugin plugin, ExecutorService executor) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.executor = executor;
    }

    /**
     * Runs a blocking task asynchronously and returns a CompletableFuture.
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Database operation failed", e);
                throw new DatabaseTaskException("Database operation failed", e);
            }
        }, executor);
    }

    /**
     * Runs a blocking task asynchronously and returns a CompletableFuture.
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Database operation failed", e);
                throw new DatabaseTaskException("Database operation failed", e);
            }
        }, executor);
    }

    /**
     * Schedules the completion of the given future to run on the main server thread.
     */
    public <T> CompletableFuture<T> sync(CompletableFuture<T> future) {
        return future.thenApplyAsync(result -> result, r -> Bukkit.getScheduler().runTask(plugin, r));
    }

    /**
     * Schedules the completion of the given future to run on the main server thread,
     * accepting both success and failure handlers.
     */
    public <T> void sync(CompletableFuture<T> future, java.util.function.Consumer<T> onSuccess,
                         java.util.function.Consumer<Throwable> onFailure) {
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(result));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept(unwrap(throwable)));
            }
        });
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof DatabaseTaskException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Shuts down the executor gracefully.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
