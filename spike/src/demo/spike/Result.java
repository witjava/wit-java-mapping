package demo.spike;

/**
 * Local copy of the support {@code Result} (io.github.witjava.support.Result,
 * mapping v1 section 8.1) with two small conveniences ({@link #unwrap()},
 * {@link #unwrapErr()}) so the spike needs no support artifact on the
 * classpath. Shape-identical otherwise.
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    default boolean isOk() {
        return this instanceof Ok;
    }

    @SuppressWarnings("unchecked")
    default T unwrap() {
        return ((Ok<T, E>) this).value;
    }

    @SuppressWarnings("unchecked")
    default E unwrapErr() {
        return ((Err<T, E>) this).error;
    }

    record Ok<T, E>(T value) implements Result<T, E> {}

    record Err<T, E>(E error) implements Result<T, E> {}
}
