package org.example;

import org.example.pool.PoolManager;
import org.example.pool.Request;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;


public class Main {
    public static void main(String[] args) {
        testThreadPool(5, 100, 100);
        testThreadPool(5, 1000, 100);
        testThreadPool(5, 10000, 100);
        testThreadPool(5, 100000, 100);
    }


    //метод для тестирования
    public static void testThreadPool(int poolSize, int requestCount, int N) {
        /* будем увеличивать n на 1 при каждом вызове метода onFinish() для того,
           чтобы знать когда выполнится нужное количество запросов
        */
        AtomicInteger n = new AtomicInteger(); //потокобезопасный Integer

        //параллельное выполнение
        long start = new Date().getTime();
        for (int i = 0; i < requestCount; i++) {
            Request request = new Request() {
                @Override
                public void execute() {
                    Math.tan(N);
                }

                @Override
                public void onFinish() {
                    n.incrementAndGet();
                }
            };

            Thread thread = new Thread(() -> {
                request.execute();
                request.onFinish();
            });
            thread.start();
        }
        while (n.get() != requestCount) ;
        long stop = new Date().getTime();
        System.out.println("parallel {requestCount=" + requestCount + "; counter n=" + N + "} execution time: " + (stop - start) + " ms");
        n.set(0);

        //пул потоков
        start = new Date().getTime();
        PoolManager manager = new PoolManager(poolSize);
        for (int i = 0; i < requestCount; i++) {
            Request request = new Request() {
                @Override
                public void execute() {
                    Math.tan(N);
                }

                @Override
                public void onFinish() {
                    n.incrementAndGet();
                }
            };
            manager.request(request);
        }
        while (n.get() != requestCount);
        stop = new Date().getTime();
        System.out.println("Thread pool {requestCount=" + requestCount + "; counter n=" + N + "; pool size: " + poolSize + "} execution time: " + (stop - start) + " ms");
        manager.shutdown();
    }

}