# M2 spike — resource declaration shape vs. a real lowering layer

**Verdict: the pure-`interface` resource mapping (spec §5.11) was NOT
falsified.** All scenarios pass with **zero modifications** to the
hand-written declarations. `run.sh` reproduces everything.

## The question (spec §19 M2)

WIT resources map to `interface X extends AutoCloseable` with no place for a
native handle. A lowering layer must attach runtime state (handle tables,
memory access) to those declarations. If the shape forced the lowering to
modify any declaration — or demanded information the declaration cannot
carry — the mapping could not freeze.

## What was run

- **Declarations** (`src/demo/spike/Wit.java`): a `wasi:io/streams` subset —
  `InputStream`/`OutputStream`/`Error` resources (interfaces + `close()`),
  the `StreamError` variant with a *resource payload*
  (`LastOperationFailed(Error)`), `Result` in return position, `list<u8>`
  → `byte[]`, an unsigned `u64` parameter (`read(len)`).
- **Lowering** (`src/demo/spike/Lowering.java`): the adapter layer —
  handle table (`IdentityHashMap`-backed), memory reads/writes, export
  calls, lifting of result structs into `Result`/variant records.
  Implements the declarations; consumes **only** them plus Chicory's
  `Instance`/`Memory`/exports.
- **Guest** (`wat/streams.wat`): a hand-written core-wasm module standing in
  for the component's canonical-ABI core, compiled at runtime with
  chicory-wabt, run on Chicory 1.4.0.
- **Driver** (`src/demo/spike/Main.java`): write/flush + byte round-trip,
  unsigned `u64` value 2^32 surviving as `long`, `read` at EOF →
  `Err(Closed)`, write-after-close → `Err(Closed)`, idempotent `close()`,
  guest failure → `Err(LastOperationFailed(error))` with
  `toDebugMessage()` round-trip, closing the error resource twice.

## Findings (the "wishes" log)

1. **The handle table fits entirely in the adapter.** Guest→Java direction
   wraps handles in adapter objects; Java→guest direction (needed for
   `borrow<T>`/`own<T>` parameters) uses the object registry. Neither
   direction wants a handle *field* or accessor on the declaration. The
   §5.11 premise that worried the design doc — "no place to hang the
   native handle" — did not materialize.
2. **`close()` narrowing is sufficient.** `void close()` (no
   `throws`) + the idempotency clause in the spec is everything the
   adapter needs; idempotency is enforced adapter-side by construction.
3. **Variant shapes lift mechanically.** Fixed nested-record names and the
   `value` payload name meant the lowering could consume `StreamError`
   without any guessing or per-case adapter code beyond construction.
4. **Nothing was missing.** No scenario required a declaration change; the
   lowering never needed metadata the declarations could not provide.

## Honest degradation note

Chicory has **no component-model runtime** today (the exploration in
dylibso/chicory#1295 was closed 2026-05-27 as a fork PoC, explicitly not
merge-bound). The guest here is therefore a hand-written core-wasm module
with a **simplified result-struct ABI** (pointer to
`[discriminant][payload]` structs) instead of the full canonical ABI
(flat lifting, `cabi_realloc`, `cabi_post_return`).

This does not weaken the verdict for the question asked: the declaration
shape is consumed identically either way — the canonical ABI lives
entirely in the adapter and never reaches the declarations. But the spike
does NOT validate full canonical-ABI feasibility on Chicory; that remains
the future adapter's work, as planned (spec §1, §17).

## Reproduce

```sh
spike/run.sh    # downloads Chicory jars once, compiles, runs all scenarios
```
