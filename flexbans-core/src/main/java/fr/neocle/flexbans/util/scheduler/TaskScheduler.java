package fr.neocle.flexbans.util;

import fr.neocle.flexbans.logger.FlexLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {
    private final ScheduledExecutorService executor;

    private static final TaskScheduler INSTANCE = new TaskScheduler();
    private static final FlexLogger LOGGER = FlexLogger.get(TaskScheduler.class);

    private TaskScheduler() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("FlexBans-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static TaskScheduler get() {
        return INSTANCE;
    }

    public void runLater(Runnable task, long delayMillis) {
        executor.schedule(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("Error in runLater task: ", t);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    public void runRepeating(Runnable task, long periodMillis) {
        executor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("Error in runRepeating task: ", t);
            }
        }, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Tasks did not finish in time");
            }
            LOGGER.info("Shutdown successful");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}