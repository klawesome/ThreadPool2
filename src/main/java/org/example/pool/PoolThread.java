package org.example.pool;

import java.util.concurrent.SynchronousQueue;

public class PoolThread {
    //исполняемы потоком код в потокобезопасной обертке
    private final SynchronousQueue<Runnable> executionCode = new SynchronousQueue<>();
    //менеджер пула, которому пренадлежит поток пула
    private final PoolManager manager;
    //поток, исполняющий код
    private final Thread poolThread;
    private volatile boolean shutdown = false;

    public PoolThread(PoolManager manager) {
        this.manager = manager;
        poolThread = new Thread(this::run);
        poolThread.start();
    }


    //метод, запускаемый потоком
    private void run() {
        manager.free(this);
        while (!shutdown) {
            try {
                Runnable execution = executionCode.take(); //ожидания кода для выполнения
                execution.run();    //запуск кода для выполнения
                manager.free(this); //добавляем поток пула в очередь свободных потоков менеджера
            }
            catch (InterruptedException e) {
            }
        }
    }

    public synchronized void shutdown() {
        shutdown = true;
        poolThread.interrupt(); //прерываем выполнение потока
    }

    //передача потоку кода для выполнения
    public synchronized void execute(Runnable runnable) {
        try {
            executionCode.put(runnable);
        } catch (InterruptedException ignored) {
        }
    }

}
