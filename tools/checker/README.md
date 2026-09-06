# Tier 1 conformance checker (planned — phase B3)

JDK-only tool (zero third-party dependencies, ~300 lines) that:

1. walks a directory of generated `.java` files,
2. parses them with `com.sun.source` (Compiler Tree API) — parsing only, no
   compilation, no WIT input,
3. emits a canonical API-shape digest: packages, type names, kinds,
   supertypes, member signatures, modifiers — normalized and sorted.

Because the checker knows nothing about WIT, any mapping implementation can
self-test against `conformance/` by comparing digests.
