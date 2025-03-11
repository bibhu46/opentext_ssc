package com.opentext;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

    public static void main(String[] args) {
        // Create an instance of the TaskExecutorService with max concurrency
        TaskExecutorService taskExecutorService = new TaskExecutorService(4);  // Set max concurrency to 3

        // Create some task groups
        TaskGroup group1 = new TaskGroup(UUID.randomUUID());
        TaskGroup group2 = new TaskGroup(UUID.randomUUID());

        // Create tasks with different task groups
        Task<String> task1 = new Task<>(UUID.randomUUID(), group1, TaskType.READ, () -> {
            Thread.sleep(1000); // Simulate task duration as action
            return "Task 1 Result";
        });

        Task<String> task2 = new Task<>(UUID.randomUUID(), group1, TaskType.WRITE, () -> {
            Thread.sleep(500); // Simulate task duration
            return "Task 2 Result";
        });

        Task<String> task3 = new Task<>(UUID.randomUUID(), group2, TaskType.READ, () -> {
            Thread.sleep(1500); // Simulate task duration
            return "Task 3 Result";
        });

        Task<String> task4 = new Task<>(UUID.randomUUID(), group2, TaskType.WRITE, () -> {
            Thread.sleep(700); // Simulate task duration
            return "Task 4 Result";
        });


        // Submit tasks to the executor
        try {
            Future<String> result1 = taskExecutorService.submitTask(task1);
            Future<String> result2 = taskExecutorService.submitTask(task2);
            Future<String> result3 = taskExecutorService.submitTask(task3);
            Future<String> result4 = taskExecutorService.submitTask(task4);

            // Wait and retrieve results
            System.out.println(result1.get()); // Should print "Task 1 Result"
            System.out.println(result2.get()); // Should print "Task 2 Result"
            System.out.println(result3.get()); // Should print "Task 3 Result"
            System.out.println(result4.get()); // Should print "Task 4 Result"
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            // Shutdown the executor after use
            taskExecutorService.shutdown();
        }
    }
}

