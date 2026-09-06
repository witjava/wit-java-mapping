package demo.spike;

import demo.spike.Result;

/**
 * Hand-written Java declarations for a wasi:io/streams subset, shaped exactly
 * as the mapping v1 draft (wit-java-mapping spec section 5) would emit them.
 *
 * <p>This file is the subject of the M2 spike: the lowering layer must work
 * against these declarations WITHOUT MODIFYING THEM.
 */
public final class Wit {
    private Wit() {}

    /// A stream error (WIT variant).
    public sealed interface StreamError
            permits StreamError.LastOperationFailed, StreamError.Closed {

        /// The last operation failed before completion; the stream is closed.
        record LastOperationFailed(Error value) implements StreamError {}

        /// The stream is closed.
        record Closed() implements StreamError {}
    }

    /// A WIT {@code error} resource.
    public interface Error extends AutoCloseable {

        /// The debug message.
        String toDebugMessage();

        /// Close. Idempotent; corresponds to the WIT resource drop.
        @Override
        void close();
    }

    /// A WIT {@code input-stream} resource.
    public interface InputStream extends AutoCloseable {

        /// Read up to {@code len} bytes.
        ///
        /// @param len Unsigned 64-bit integer, range 0..2^64-1, stored as a
        ///            Java {@code long}, which is signed. Use
        ///            {@link java.lang.Long#compareUnsigned},
        ///            {@link java.lang.Long#divideUnsigned} and
        ///            {@link java.lang.Long#toUnsignedString} for unsigned
        ///            operations.
        /// @return read
        Result<byte[], StreamError> read(long len);

        /// Close. Idempotent; corresponds to the WIT resource drop.
        @Override
        void close();
    }

    /// A WIT {@code output-stream} resource.
    public interface OutputStream extends AutoCloseable {

        /// How many bytes may still be written.
        ///
        /// @return check-write
        Result<Long, StreamError> checkWrite();

        /// Write {@code contents}.
        Result<Unit0, StreamError> write(byte[] contents);

        /// Flush.
        Result<Unit0, StreamError> flush();

        /// Close. Idempotent; corresponds to the WIT resource drop.
        @Override
        void close();
    }

    /// Local stand-in for support {@code Unit} (kept self-contained so the
    /// spike needs no support artifact on the classpath).
    public enum Unit0 { UNIT }
}
