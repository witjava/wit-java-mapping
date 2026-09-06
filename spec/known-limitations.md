# Known limitations — mapping v1

Every semantic loss must be recorded here before the spec freezes (M1 gate).

1. **`u64` unsigned semantics lost** — maps to `long`. Generated Javadoc on
   every `u64`-derived declaration must point to `Long.compareUnsigned` /
   `Long.toUnsignedString` / `Long.divideUnsigned`.
2. **`own<T>` vs `borrow<T>` indistinguishable** — both map to the same Java
   type; the ownership contract lives in Javadoc only, never in types.
3. **NaN semantics** — WIT specifies canonical NaN; Java comparison semantics
   differ. Noted once in the support package's `package-info.java`, not per
   declaration.
4. **`Optional` in non-return positions** — a deliberate violation of Java API
   conventions (`java.util.Optional` in record components / parameters) to
   preserve `option<option<T>>`; the `nullable` style rejects that case
   instead (WJ0006).
5. **Type aliases erased** (R3) — `type x = y` and `use a.{b as c}` resolve to
   the target type; no Java type is generated for the alias. Confirmed
   readability loss on real WIT: `wasi:sockets`' `ipv4-address` /
   `ipv6-address` (aliases to `tuple<u8, x4>` / `tuple<u16, x8>`) appear in
   Java signatures as positional `Tuple4<Integer, …>`, and `wasi:filesystem`'s
   `filesize` / `link-count` appear as bare `long`.

Recorded during the B2 walkthrough (2026-09-06, `wasi:filesystem@0.2.8`,
`wasi:sockets@0.2.8`, `wasi:io@0.2.8`):

6. **Variant payloads can carry resources** (e.g. `stream-error`'s
   `last-operation-failed(error)`): the payload maps to the resource's Java
   interface type with no ownership signal in the Java type system; the
   §5.11 nested-owned-handles Javadoc line applies.
7. **@unstable / @since density is high in real WASI** (44 annotated items in
   `wasi:filesystem/types` alone): with default feature settings, generated
   output is *much* smaller than the WIT source suggests. Not a defect —
   documented so users do not report missing items as generator bugs.
