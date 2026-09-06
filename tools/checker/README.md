# Tier 1 conformance checker

JDK-only tool (zero third-party dependencies, single source file) that:

1. walks a directory of generated `.java` files,
2. parses them with `com.sun.source` (Compiler Tree API) — parsing only, no
   compilation, no WIT input,
3. emits a canonical API-shape digest: packages, type names, kinds,
   supertypes, member signatures, modifiers — normalized and sorted.

Because the checker knows nothing about WIT, any mapping implementation can
self-test against `conformance/` by comparing digests.

```sh
java tools/checker/Checker.java <dir>            # print a directory's digest
java tools/checker/Checker.java --compare A B    # exit 0 iff digests equal
java tools/checker/Checker.java --selftest       # built-in self-test (run in CI)
```

The reference implementation consumes this checker (and the `conformance/`
data, vendored per mapping version) in its own test suite — see
`wit-java` → `crates/wit-java-core/tests/conformance.rs`.
