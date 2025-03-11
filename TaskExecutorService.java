package com.opentext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskExecutorService implements TaskExecutor{

    private final ExecutorService executor;
    private final Map<TaskGroup, Object> groupLocks;
    private final Queue<Task<?>> taskQueue;
    private final int maxConcurrency;
    private final Map<UUID, CompletableFuture<?>> taskFutures; // To store Futures for tasks

    public TaskExecutorService(int maxConcurrency) {
        this.executor = Executors.newFixedThreadPool(maxConcurrency);
        this.groupLocks = new HashMap<>();
        this.taskQueue = new LinkedList<>();
        this.taskFutures = new HashMap<>();
        this.maxConcurrency = maxConcurrency;
    }

    public <T> Future<T> submitTask(Task<T> task) {
        // Create a wrapper CompletableFuture to track the result
        CompletableFuture<T> future = new CompletableFuture<>();
        taskFutures.put(task.taskUUID(), future);

        // Synchronize tasks by task group
        synchronized (getGroupLock(task.taskGroup())) {
            taskQueue.offer(task);
            executeNextTask();
        }

        return future;
    }

    private <T> void executeNextTask() {
        Task<?> task = taskQueue.poll();
        if (task != null) {
            executor.submit(() -> {
                try {
                    // Execute the task
                    Object result = task.taskAction().call();

                    // Get the corresponding future and complete it
                    CompletableFuture<Object> future = (CompletableFuture<Object>) taskFutures.get(task.taskUUID());
                    future.complete(result);
                } catch (Exception e) {
                    // If there is an exception, complete the future exceptionally
                    CompletableFuture<Object> future = (CompletableFuture<Object>) taskFutures.get(task.taskUUID());
                    future.completeExceptionally(e);
                } finally {
                    // After finishing this task, check if we can execute the next one
                    synchronized (getGroupLock(task.taskGroup())) {
                        executeNextTask();
                    }
                }
            });
        }
    }

    private Object getGroupLock(TaskGroup group) {
        synchronized (groupLocks) {
            return groupLocks.computeIfAbsent(group, k -> new Object());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

}
