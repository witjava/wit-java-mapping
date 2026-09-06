package demo.spike;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import demo.spike.Wit.StreamError;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The adapter layer under test: implements the declarations from {@link Wit}
 * over Chicory's core runtime. This file mimics what a Chicory-based
 * component adapter would write ONCE, generically, for any component.
 *
 * <p>M2 evidence rule: every time this file "wished" a declaration had
 * something it does not have, that goes into spike/README.md. Success:
 * zero modifications to Wit.java.
 */
final class Lowering {

    /** Java object <-> guest handle table (runtime concern, not in declarations). */
    static final class HandleTable {
        private final Map<Object, Integer> toHandle = new HashMap<>();
        private final Map<Integer, Object> toObject = new HashMap<>();
        private int next = 1;

        int register(Object obj) {
            int h = next++;
            toHandle.put(obj, h);
            toObject.put(h, obj);
            return h;
        }

        Object lookup(int handle) {
            return toObject.get(handle);
        }
    }

    private final Instance instance;
    private final Memory mem;
    private final HandleTable table = new HandleTable();

    Lowering(Instance instance) {
        this.instance = instance;
        this.mem = instance.memory();
    }

    private long[] call(String name, long... args) {
        return instance.export(name).apply(args);
    }

    private int readI32(int ptr) {
        return ByteBuffer.wrap(mem.readBytes(ptr, 4))
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private long readI64(int ptr) {
        return ByteBuffer.wrap(mem.readBytes(ptr, 8))
                .order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    // ------------------------------------------------------------ resources

    final class GuestError implements Wit.Error {
        private final int handle;
        private boolean dropped;

        GuestError(int handle) {
            this.handle = handle;
            table.register(this); // obj->handle registration works without any
                                  // handle field in the declaration
        }

        @Override
        public String toDebugMessage() {
            int ptr = (int) call("error-debug", handle)[0];
            int len = readI32(ptr);
            return new String(mem.readBytes(ptr + 4, len), StandardCharsets.UTF_8);
        }

        @Override
        public void close() {
            if (!dropped) { // idempotent by construction
                dropped = true;
                call("error-drop", handle);
            }
        }
    }

    final class GuestInputStream implements Wit.InputStream {
        private final int handle;
        private boolean dropped;

        GuestInputStream(int handle) {
            this.handle = handle;
        }

        @Override
        public Result<byte[], StreamError> read(long len) {
            int ptr = (int) call("istream-read", handle, len)[0];
            int disc = readI32(ptr);
            if (disc == 0) {
                int n = readI32(ptr + 4);
                return Result.ok(mem.readBytes(ptr + 8, n));
            }
            return Result.err(decodeError(ptr));
        }

        @Override
        public void close() {
            if (!dropped) {
                dropped = true;
                call("istream-drop", handle);
            }
        }
    }

    final class GuestOutputStream implements Wit.OutputStream {
        private final int handle;
        private boolean dropped;

        GuestOutputStream(int handle) {
            this.handle = handle;
        }

        @Override
        public Result<Long, StreamError> checkWrite() {
            int ptr = (int) call("ostream-check-write", handle)[0];
            if (readI32(ptr) == 0) {
                return Result.ok(readI64(ptr + 4));
            }
            return Result.err(decodeError(ptr));
        }

        @Override
        public Result<Wit.Unit0, StreamError> write(byte[] contents) {
            int data = (int) call("alloc", contents.length)[0];
            mem.write(data, contents);
            int ptr = (int) call("ostream-write", handle, data, contents.length)[0];
            return decodeUnit(ptr);
        }

        @Override
        public Result<Wit.Unit0, StreamError> flush() {
            return decodeUnit((int) call("ostream-flush", handle)[0]);
        }

        @Override
        public void close() {
            if (!dropped) {
                dropped = true;
                call("ostream-drop", handle);
            }
        }
    }

    // -------------------------------------------------------------- lifting

    private StreamError decodeError(int ptr) {
        int disc = readI32(ptr);
        if (disc == 1) {
            return new StreamError.Closed();
        }
        if (disc == 2) {
            return new StreamError.LastOperationFailed(new GuestError(readI32(ptr + 4)));
        }
        throw new IllegalStateException("bad result discriminant " + disc);
    }

    private Result<Wit.Unit0, StreamError> decodeUnit(int ptr) {
        if (readI32(ptr) == 0) {
            return Result.ok(Wit.Unit0.UNIT);
        }
        return Result.err(decodeError(ptr));
    }

    // ------------------------------------------------- factories + test aid

    Wit.InputStream openInput() {
        return new GuestInputStream((int) call("istream-open")[0]);
    }

    Wit.OutputStream openOutput() {
        return new GuestOutputStream((int) call("ostream-open")[0]);
    }

    /** Test-only: guest dump export, decoded ([len][bytes]). */
    byte[] dumpOutput() {
        int ptr = (int) call("ostream-dump", 2)[0];
        int len = readI32(ptr);
        return mem.readBytes(ptr + 4, len);
    }

    void armFail() {
        call("arm-fail");
    }
}
