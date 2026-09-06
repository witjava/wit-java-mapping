# wit-java-mapping

**WIT → Java mapping specification.** This repository is a specification, not an
implementation.

It defines how WebAssembly Interface Types (WIT) packages map onto Java 17
declarations — types, names, packages, documentation — so that multiple
independent tools can produce **interchangeable** Java APIs for the same WIT
world. The reference implementation lives at
[`witjava/wit-java`](https://github.com/witjava/wit-java).

| path | content |
|---|---|
| `spec/v1.md` | the mapping (draft) |
| `spec/error-codes.md` | diagnostic code registry |
| `spec/known-limitations.md` | recorded semantic losses |
| `conformance/` | self-test data for any implementation |
| `tools/checker/` | Tier-1 API-shape checker (JDK-only, zero dependencies) |
| `spike/` | M2 spike: resource declaration shape vs. a real lowering layer |

Status: **draft, not frozen**. After freeze the spec is append-only (`vN` can
only be extended, never modified); the support-package FQN is part of the spec,
and changing it is a breaking change.

License: Apache-2.0 WITH LLVM-exception OR Apache-2.0 OR MIT
(see `LICENSE-APACHE` / `LICENSE-MIT`).
