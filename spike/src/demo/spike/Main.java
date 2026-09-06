package demo.spike;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import demo.spike.Wit.InputStream;
import demo.spike.Wit.OutputStream;
import demo.spike.Wit.StreamError;

import java.nio.charset.StandardCharsets;

/**
 * M2 spike driver: compiles streams.wat (via chicory-wabt), instantiates it
 * in Chicory, and drives the hand-written declarations through the lowering.
 * Success criterion: every scenario passes with ZERO edits to Wit.java.
 */
public final class Main {
    private int failures;

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    private void run() throws Exception {
        byte[] wat;
        try (var in = getClass().getResourceAsStream("/streams.wat")) {
            wat = in.readAllBytes();
        }
        WasmModule module = Parser.parse(Wat2Wasm.parse(
                new String(wat, StandardCharsets.UTF_8)));
        Instance instance = Instance.builder(module)
                .withInitialize(true).build();
        Lowering lower = new Lowering(instance);

        // A: write + flush, verify bytes in guest memory via test export.
        OutputStream out = lower.openOutput();
        byte[] payload = "wit-java".getBytes(StandardCharsets.UTF_8);
        check("A1 write ok", out.write(payload).isOk());
        check("A2 flush ok", out.flush().isOk());
        check("A3 bytes round-trip",
                new String(lower.dumpOutput(), StandardCharsets.UTF_8).equals("wit-java"));

        // B: check-write returns an unsigned u64 (2^32) as long.
        check("B1 u64 unsigned value", out.checkWrite().unwrap() == 4294967296L);

        // C: read returns the embedded data.
        InputStream in = lower.openInput();
        check("C1 read 5", new String(in.read(5).unwrap(), StandardCharsets.UTF_8).equals("hello"));

        // D: read past EOF -> Err(Closed).
        in.read(100); // consume the rest; guest marks EOF
        check("D1 read closed", in.read(10).unwrapErr() instanceof StreamError.Closed);

        // E: write after close -> Err(Closed); close is idempotent.
        out.close();
        out.close(); // must not throw
        check("E1 write closed", out.write(payload).unwrapErr() instanceof StreamError.Closed);

        // F: guest-induced failure -> Err(LastOperationFailed(Error)).
        OutputStream out2 = lower.openOutput();
        lower.armFail();
        StreamError ferr = out2.write(payload).unwrapErr();
        check("F1 last-op-failed", ferr instanceof StreamError.LastOperationFailed);
        if (ferr instanceof StreamError.LastOperationFailed f) {
            check("F2 error message", f.value().toDebugMessage().equals("guest io failure"));
            f.value().close();
            f.value().close(); // idempotent
        }

        System.out.println(failures == 0
                ? "M2 SPIKE: ALL SCENARIOS PASS — declarations unmodified"
                : "M2 SPIKE: " + failures + " FAILURES");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
