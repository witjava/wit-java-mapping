# Conformance data — mapping v1

Self-test data for **any** implementation of this spec, in any language.

## Case layout

Each case lives in `cases/<name>/`:

```text
cases/<name>/src.wit            input: single WIT file, OR
cases/<name>/src/               input: WIT directory (package + deps/)
cases/<name>/case.toml          metadata + CLI options
cases/<name>/packages.toml      optional: --package-map file (resolved
                                relative to the case directory)
cases/<name>/expected/          positive only: expected output tree
cases/<name>/expected-error.toml  negative only: expected error code
```

`case.toml`:

```toml
name = "primitives"
description = "what the case pins down"
options = ["--no-support"]      # CLI options verbatim (no --out/--world)
```

`expected-error.toml`:

```toml
code = "WJ0001"                 # from spec/error-codes.md
```

## Runner contract

```text
wit-java generate <src> --out <tmp> [case options]
java tools/checker/Checker.java --compare cases/<name>/expected <tmp>
```

Positive: digest equal AND exit code 0. Negative: non-zero exit, stderr names
the code, and `--out` (if given) stays empty — a failing run MUST NOT write.

## What the digest compares (Tier 1)

Compared: packages; type names, kinds, type parameters; record components
(order — spec-mandated); enum constants (order — spec-mandated); permits
(order — spec-mandated); supertypes; static fields; method signatures
(name, canonicalized parameter types and names, return type, staticness,
declaration annotations such as `@Nullable`).

Not compared: file split and file names; import statements; comments and
Javadoc prose; the `@generated` header; declaration order; method order
within a type; whitespace; line endings; `<tool-ver>` in headers.

Type references are canonicalized via each file's imports plus the set of
declared types, so a simple-name style and an FQN style digest identically.
The checker parses but does not compile; it knows nothing about WIT.

A reference runner lives in the implementation repository
(`wit-java` → `crates/wit-java-core/tests/conformance.rs`): it consumes a
vendored snapshot of this data, generates each case, and compares Tier-1
digests through `tools/checker/Checker.java`.
