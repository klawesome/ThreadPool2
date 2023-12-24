package org.example.pool;

import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PoolManager {
    private final Vector<PoolThread> pool = new Vector<>(); //потоки пула
    private final BlockingQueue<PoolThread> free = new LinkedBlockingQueue<>(); //очередь свободных потоков
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>(); //очередь запросов
    private final Thread handler; //распределитель запросов по потокам пула
    private volatile boolean shutdown = false;

    public PoolManager(int size) {
        if (size < 1) throw new IllegalArgumentException("The number of threads must be greater than zero");
        for (int i = 0; i < size; i++) {
            pool.add(new PoolThread(this));
        }
        handler = new Thread(this::run);
        handler.start();
    }

    //метод, запускаемый потоком handler
    private void run() {
        //выполняется, пока не будет прерван
        while (!handler.isInterrupted()) {
            try {
                //ждем свободный поток и запрос
                PoolThread thread = free.take();
                Request request = requests.take();
                //реализуем Runnable на основе запроса и передаем его в поток.
                thread.execute(() -> {
                    try {
                        request.execute();
                        request.onFinish();
                    } catch (Exception exception) {
                        // в случае ошибки при выполнении одного из методов вызываем onException()
                        request.onException();
                    }
                });
            } catch (InterruptedException interruptedException) {
                handler.interrupt();
            }
        }
    }

    public synchronized void shutdown() {
        if (shutdown) return;
        shutdown = true;
        handler.interrupt(); //прерываем поток
        requests.forEach(Request::onCancel); //вызываем метод отмены для запросов в очереди
        pool.forEach(PoolThread::shutdown); //вызываем метод выключения для каждого потока
    }

    public synchronized void shutdown(Runnable runnable) {
        if (shutdown) return;
        shutdown();
        runnable.run();
    }


    //добавляет поток пула в очередь свободных
    public synchronized void free(PoolThread thread) {
        try {
            this.free.put(thread);
        } catch (InterruptedException ignored) {
        }
    }

    //добавляет запрос в очередь
    public synchronized void request(Request request) {
        requests.add(request);
    }

}
