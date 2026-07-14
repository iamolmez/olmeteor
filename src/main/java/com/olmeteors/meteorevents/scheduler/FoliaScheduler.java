package com.olmeteors.meteorevents.scheduler;

import com.olmeteors.meteorevents.MeteorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A comprehensive scheduler wrapper that provides unified access to
 * Paper's new threading APIs (RegionScheduler, AsyncScheduler,
 * GlobalRegionScheduler) with full Folia compatibility.
 * <p>
 * Falls back gracefully to standard BukkitRunnable on non-Folia servers.
 */
public final class FoliaScheduler {

    private final MeteorPlugin plugin;
    private final boolean folia;
    private final ConcurrentMap<Integer, ScheduledTask> activeTasks;

    private static final java.util.concurrent.ExecutorService VIRTUAL_EXECUTOR = 
        java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

    public FoliaScheduler(MeteorPlugin plugin) {
        this.plugin = plugin;
        this.folia = MeteorPlugin.isFolia();
        this.activeTasks = new ConcurrentHashMap<>();
    }

    /**
     * Schedules a task to run on the region (world) thread for the given location.
     * This is the primary scheduling method for most gameplay-related tasks.
     *
     * @param location the location to schedule the task for
     * @param task     the task to execute
     * @return a ScheduledTask handle
     */
    public @NotNull ScheduledTask runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        if (folia) {
            final var future = new CompletableFuture<Void>();
            Bukkit.getRegionScheduler().execute(plugin, location, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            final var scheduledTask = new ScheduledTask(future, true);
            trackTask(scheduledTask);
            return scheduledTask;
        } else {
            final var taskId = Bukkit.getScheduler().runTask(plugin, task).getTaskId();
            final var scheduledTask = new ScheduledTask(taskId, false);
            trackTask(scheduledTask);
            return scheduledTask;
        }
    }

    /**
     * Schedules a task to run on the region thread for a specific entity.
     *
     * @param entity the entity whose region thread to use
     * @param task   the task to execute
     * @return a ScheduledTask handle
     */
    public @NotNull ScheduledTask runForEntity(@NotNull Entity entity, @NotNull Runnable task) {
        if (folia) {
            final var future = new CompletableFuture<Void>();
            entity.getScheduler().execute(plugin, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }, null, 0L);
            return new ScheduledTask(future, true);
        } else {
            return runAtLocation(entity.getLocation(), task);
        }
    }

    /** Runs a repeating task on an entity's owning scheduler, following it across regions. */
    public @NotNull ScheduledTask runRepeatingForEntity(@NotNull Entity entity,
                                                         @NotNull Runnable task,
                                                         long delay, long period) {
        if (folia) {
            final var foliaTask = entity.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
                try {
                    task.run();
                } catch (Exception error) {
                    scheduledTask.cancel();
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Repeating entity task failed for " + entity.getUniqueId(), error);
                }
            }, null, Math.max(1L, delay), Math.max(1L, period));
            final var scheduledTask = new ScheduledTask(foliaTask, true);
            trackTask(scheduledTask);
            return scheduledTask;
        }
        final var taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).getTaskId();
        final var scheduledTask = new ScheduledTask(taskId, false);
        trackTask(scheduledTask);
        return scheduledTask;
    }

    /**
     * Schedules a task to run asynchronously using virtual threads.
     *
     * @param task the task to execute
     * @return a ScheduledTask handle
     */
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        if (folia) {
            final var future = CompletableFuture.runAsync(task, VIRTUAL_EXECUTOR);
            final var scheduledTask = new ScheduledTask(future, true);
            trackTask(scheduledTask);
            return scheduledTask;
        } else {
            final var taskId = Bukkit.getScheduler().runTaskAsynchronously(plugin, task).getTaskId();
            final var scheduledTask = new ScheduledTask(taskId, true);
            trackTask(scheduledTask);
            return scheduledTask;
        }
    }

    /**
     * Schedules a delayed task on the global region thread.
     *
     * @param task   the task to execute
     * @param delay  the delay in ticks
     * @return a ScheduledTask handle
     */
    public @NotNull ScheduledTask runLaterGlobal(@NotNull Runnable task, long delay) {
        if (folia) {
            final var foliaTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Delayed global task failed", e);
                }
            }, delay);
            return new ScheduledTask(foliaTask, true);
        } else {
            final var taskId = Bukkit.getScheduler().runTaskLater(plugin, task, delay).getTaskId();
            return new ScheduledTask(taskId, false);
        }
    }

    /**
     * Schedules a delayed task on the region thread for the given location.
     *
     * @param location the location to schedule for
     * @param task     the task
     * @param delay    delay in ticks
     * @return ScheduledTask handle
     */
    public @NotNull ScheduledTask runLaterAtLocation(@NotNull Location location, @NotNull Runnable task, long delay) {
        if (folia) {
            final var foliaTask = Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> {
                try {
                    task.run();
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                            "Delayed region task failed at " + location, e);
                }
            }, delay);
            return new ScheduledTask(foliaTask, true);
        } else {
            final var taskId = Bukkit.getScheduler().runTaskLater(plugin, task, delay).getTaskId();
            return new ScheduledTask(taskId, false);
        }
    }

    /**
     * Schedules a repeating task on the global region thread.
     *
     * @param task     the task to execute
     * @param delay    initial delay in ticks
     * @param period   period between executions in ticks
     * @return a ScheduledTask handle
     */
    public @NotNull ScheduledTask runRepeatingGlobal(@NotNull Runnable task, long delay, long period) {
        if (folia) {
            final var future = new CompletableFuture<Void>();
            final var taskReference = new Object() { ScheduledTask scheduledTask; };
            final var consumer = new Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>() {
                private boolean cancelled = false;

                @Override
                public void accept(io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
                    if (cancelled) return;
                    try {
                        task.run();
                    } catch (Exception e) {
                        cancelled = true;
                        foliaTask.cancel();
                        future.completeExceptionally(e);
                    }
                }
            };
            final var foliaTask = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, consumer, delay, period);
            return new ScheduledTask(foliaTask, true);
        } else {
            final var taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).getTaskId();
            return new ScheduledTask(taskId, false);
        }
    }

    /**
     * Schedules a repeating task on the region thread for the given location.
     *
     * @param location the location to schedule for
     * @param task     the task
     * @param delay    initial delay in ticks
     * @param period   period in ticks
     * @return ScheduledTask handle
     */
    public @NotNull ScheduledTask runRepeatingAtLocation(@NotNull Location location, @NotNull Runnable task,
                                                          long delay, long period) {
        if (folia) {
            final var future = new CompletableFuture<Void>();
            final var consumer = new Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>() {
                private boolean cancelled = false;

                @Override
                public void accept(io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask) {
                    if (cancelled) return;
                    try {
                        task.run();
                    } catch (Exception e) {
                        cancelled = true;
                        foliaTask.cancel();
                        future.completeExceptionally(e);
                    }
                }
            };
            final var foliaTask = Bukkit.getRegionScheduler()
                    .runAtFixedRate(plugin, location, consumer, delay, period);
            return new ScheduledTask(foliaTask, true);
        } else {
            final var taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).getTaskId();
            return new ScheduledTask(taskId, false);
        }
    }

    /**
     * Schedules a delayed async task.
     *
     * @param task  the task
     * @param delay delay in ticks (50ms per tick)
     * @return ScheduledTask handle
     */
    public @NotNull ScheduledTask runLaterAsync(@NotNull Runnable task, long delay) {
        final long delayMs = delay * 50L;
        if (folia) {
            final var future = CompletableFuture.runAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, VIRTUAL_EXECUTOR);
            return new ScheduledTask(future, true);
        } else {
            final var taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay).getTaskId();
            return new ScheduledTask(taskId, true);
        }
    }

    /**
     * Executes a task on the main/global thread and returns a CompletableFuture.
     *
     * @param task the task
     * @return CompletableFuture that completes when the task finishes
     */
    public @NotNull CompletableFuture<Void> callGlobal(@NotNull Runnable task) {
        final var future = new CompletableFuture<Void>();
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    task.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    /**
     * Executes a task on the region thread for the given location and returns a CompletableFuture.
     *
     * @param location the location
     * @param task     the task
     * @return CompletableFuture
     */
    public @NotNull CompletableFuture<Void> callAtLocation(@NotNull Location location, @NotNull Runnable task) {
        final var future = new CompletableFuture<Void>();
        runAtLocation(location, () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Executes a task on an async virtual thread and returns a CompletableFuture.
     *
     * @param task the task
     * @return CompletableFuture
     */
    public @NotNull CompletableFuture<Void> callAsync(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, VIRTUAL_EXECUTOR);
    }

    /**
     * Ensures all chunks in the given area are force-loaded for the event duration.
     *
     * @param world  the world
     * @param cx     chunk X center
     * @param cz     chunk Z center
     * @param radius chunk radius
     */
    public void forceLoadChunks(@NotNull World world, int cx, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                final int chunkX = cx + dx;
                final int chunkZ = cz + dz;
                final Location owner = new Location(world, (chunkX << 4) + 8,
                        world.getMinHeight(), (chunkZ << 4) + 8);
                runAtLocation(owner, () -> world.getChunkAt(chunkX, chunkZ).setForceLoaded(true));
            }
        }
    }

    /**
     * Releases force-loaded chunks in the given area.
     *
     * @param world  the world
     * @param cx     chunk X center
     * @param cz     chunk Z center
     * @param radius chunk radius
     */
    public void releaseChunks(@NotNull World world, int cx, int cz, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                final int chunkX = cx + dx;
                final int chunkZ = cz + dz;
                final Location owner = new Location(world, (chunkX << 4) + 8,
                        world.getMinHeight(), (chunkZ << 4) + 8);
                runAtLocation(owner, () -> world.getChunkAt(chunkX, chunkZ).setForceLoaded(false));
            }
        }
    }

    private void trackTask(ScheduledTask task) {
        if (task.getTaskId() >= 0) {
            activeTasks.put(task.getTaskId(), task);
        }
    }

    public void cancelTask(ScheduledTask task) {
        if (task != null) {
            task.cancel();
            activeTasks.remove(task.getTaskId());
        }
    }

    public void cancelAllTasks() {
        activeTasks.values().forEach(ScheduledTask::cancel);
        activeTasks.clear();
    }

    /**
     * A wrapper around Folia's ScheduledTask or Bukkit's task ID.
     */
    public static final class ScheduledTask {

        private final int taskId;
        private final boolean async;
        private io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask;
        private CompletableFuture<Void> future;
        private boolean cancelled;

        private static final java.util.concurrent.atomic.AtomicInteger ID_COUNTER =
                new java.util.concurrent.atomic.AtomicInteger(10000);

        // Constructors for Bukkit tasks
        ScheduledTask(int bukkitTaskId, boolean async) {
            this.taskId = bukkitTaskId;
            this.async = async;
            this.cancelled = false;
        }

        // Constructors for Folia tasks
        ScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask, boolean async) {
            this.taskId = ID_COUNTER.incrementAndGet();
            this.async = async;
            this.foliaTask = foliaTask;
            this.cancelled = false;
        }

        // Constructor for CompletableFuture-based tasks
        ScheduledTask(CompletableFuture<Void> future, boolean async) {
            this.taskId = ID_COUNTER.incrementAndGet();
            this.async = async;
            this.future = future;
            this.cancelled = false;
        }

        public int getTaskId() {
            return taskId;
        }

        public boolean isAsync() {
            return async;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void cancel() {
            if (cancelled) return;
            this.cancelled = true;

            if (foliaTask != null) {
                foliaTask.cancel();
            }
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            if (foliaTask == null && future == null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }

        public CompletableFuture<Void> asFuture() {
            return future;
        }
    }
}
