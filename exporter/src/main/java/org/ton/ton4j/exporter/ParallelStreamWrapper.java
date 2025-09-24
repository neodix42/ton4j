package org.ton.ton4j.exporter;

import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Stream;

/** Wrapper class to provide a Stream that uses a custom ForkJoinPool for parallel operations */
class ParallelStreamWrapper<T> implements Stream<T> {
  private final Stream<T> delegate;
  private final java.util.concurrent.ForkJoinPool customPool;

  public ParallelStreamWrapper(Stream<T> delegate, java.util.concurrent.ForkJoinPool customPool) {
    this.delegate = delegate;
    this.customPool = customPool;
  }

  @Override
  public Stream<T> parallel() {
    return this;
  }

  @Override
  public Stream<T> sequential() {
    return delegate.sequential();
  }

  @Override
  public boolean isParallel() {
    return true;
  }

  // Delegate all other Stream methods to the underlying stream
  @Override
  public Stream<T> filter(Predicate<? super T> predicate) {
    return new ParallelStreamWrapper<>(delegate.filter(predicate), customPool);
  }

  @Override
  public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
    return new ParallelStreamWrapper<>(delegate.map(mapper), customPool);
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return new ParallelStreamWrapper<>(delegate.flatMap(mapper), customPool);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    try {
      customPool.submit(() -> delegate.parallel().forEach(action)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      customPool.shutdown();
      try {
        if (!customPool.awaitTermination(60, TimeUnit.SECONDS)) {
          customPool.shutdownNow();
        }
      } catch (InterruptedException ex) {
        customPool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    try {
      customPool.submit(() -> delegate.parallel().forEachOrdered(action)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      customPool.shutdown();
      try {
        if (!customPool.awaitTermination(60, TimeUnit.SECONDS)) {
          customPool.shutdownNow();
        }
      } catch (InterruptedException ex) {
        customPool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public java.util.Iterator<T> iterator() {
    return delegate.iterator();
  }

  @Override
  public java.util.Spliterator<T> spliterator() {
    return delegate.spliterator();
  }

  @Override
  public long count() {
    try {
      return customPool.submit(() -> delegate.parallel().count()).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public java.util.Optional<T> min(java.util.Comparator<? super T> comparator) {
    try {
      return customPool.submit(() -> delegate.parallel().min(comparator)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public java.util.Optional<T> max(java.util.Comparator<? super T> comparator) {
    try {
      return customPool.submit(() -> delegate.parallel().max(comparator)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    try {
      return customPool.submit(() -> delegate.parallel().anyMatch(predicate)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    try {
      return customPool.submit(() -> delegate.parallel().allMatch(predicate)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    try {
      return customPool.submit(() -> delegate.parallel().noneMatch(predicate)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public java.util.Optional<T> findFirst() {
    try {
      return customPool.submit(() -> delegate.parallel().findFirst()).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public java.util.Optional<T> findAny() {
    try {
      return customPool.submit(() -> delegate.parallel().findAny()).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public Object[] toArray() {
    try {
      return customPool.submit(() -> delegate.parallel().toArray()).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    try {
      return customPool.submit(() -> delegate.parallel().toArray(generator)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    try {
      return customPool.submit(() -> delegate.parallel().reduce(identity, accumulator)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public java.util.Optional<T> reduce(BinaryOperator<T> accumulator) {
    try {
      return customPool.submit(() -> delegate.parallel().reduce(accumulator)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public <U> U reduce(
      U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
    try {
      return customPool
          .submit(() -> delegate.parallel().reduce(identity, accumulator, combiner))
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public <R> R collect(
      Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    try {
      return customPool
          .submit(() -> delegate.parallel().collect(supplier, accumulator, combiner))
          .get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) {
    try {
      return customPool.submit(() -> delegate.parallel().collect(collector)).get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // Note: Thread pool shutdown is handled by the close() method or onClose() handler
  }

  @Override
  public Stream<T> distinct() {
    return new ParallelStreamWrapper<>(delegate.distinct(), customPool);
  }

  @Override
  public Stream<T> sorted() {
    return new ParallelStreamWrapper<>(delegate.sorted(), customPool);
  }

  @Override
  public Stream<T> sorted(java.util.Comparator<? super T> comparator) {
    return new ParallelStreamWrapper<>(delegate.sorted(comparator), customPool);
  }

  @Override
  public Stream<T> peek(Consumer<? super T> action) {
    return new ParallelStreamWrapper<>(delegate.peek(action), customPool);
  }

  @Override
  public Stream<T> limit(long maxSize) {
    return new ParallelStreamWrapper<>(delegate.limit(maxSize), customPool);
  }

  @Override
  public Stream<T> skip(long n) {
    return new ParallelStreamWrapper<>(delegate.skip(n), customPool);
  }

  @Override
  public java.util.stream.IntStream mapToInt(ToIntFunction<? super T> mapper) {
    return delegate.mapToInt(mapper);
  }

  @Override
  public java.util.stream.LongStream mapToLong(ToLongFunction<? super T> mapper) {
    return delegate.mapToLong(mapper);
  }

  @Override
  public java.util.stream.DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    return delegate.mapToDouble(mapper);
  }

  @Override
  public java.util.stream.IntStream flatMapToInt(
      Function<? super T, ? extends java.util.stream.IntStream> mapper) {
    return delegate.flatMapToInt(mapper);
  }

  @Override
  public java.util.stream.LongStream flatMapToLong(
      Function<? super T, ? extends java.util.stream.LongStream> mapper) {
    return delegate.flatMapToLong(mapper);
  }

  @Override
  public java.util.stream.DoubleStream flatMapToDouble(
      Function<? super T, ? extends java.util.stream.DoubleStream> mapper) {
    return delegate.flatMapToDouble(mapper);
  }

  @Override
  public Stream<T> takeWhile(Predicate<? super T> predicate) {
    return new ParallelStreamWrapper<>(delegate.takeWhile(predicate), customPool);
  }

  @Override
  public Stream<T> dropWhile(Predicate<? super T> predicate) {
    return new ParallelStreamWrapper<>(delegate.dropWhile(predicate), customPool);
  }

  @Override
  public void close() {
    delegate.close();
    if (!customPool.isShutdown()) {
      customPool.shutdown();
      try {
        if (!customPool.awaitTermination(60, TimeUnit.SECONDS)) {
          customPool.shutdownNow();
        }
      } catch (InterruptedException ex) {
        customPool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public Stream<T> onClose(Runnable closeHandler) {
    return new ParallelStreamWrapper<>(delegate.onClose(closeHandler), customPool);
  }

  @Override
  public Stream<T> unordered() {
    return new ParallelStreamWrapper<>(delegate.unordered(), customPool);
  }
}
