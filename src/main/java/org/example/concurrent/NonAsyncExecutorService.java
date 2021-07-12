package org.example.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class NonAsyncExecutorService implements ExecutorService {
  private volatile boolean shutdown;

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown = true;
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdown;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) {
    shutdown = true;
    return true;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<?> submit(Runnable task) {
    task.run();
    return NonAsyncFutureTask.getNullable();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
      TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  @RequiredArgsConstructor
  private static class NonAsyncFutureTask<T> implements Future<T> {
    @Getter(lazy = true)
    private static final Future<?> nullable = new NonAsyncFutureTask<>(null);

    private final T result;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return true;
    }

    @Override
    public T get() {
      return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
      return result;
    }
  }
}
