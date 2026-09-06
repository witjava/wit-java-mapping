# Changelog

## [Unreleased]

- v1 draft skeleton: spec outline, error-code registry, known-limitations,
  conformance layout, checker placeholder.
- M2 spike (`spike/`): the pure-`interface` resource mapping was not
  falsified; unblocks freezing §5.11.
- Tier-1 checker implemented (`tools/checker/Checker.java`): digest,
  `--compare`, `--selftest`; wired into CI.
- Conformance suite: 14 positive + 7 negative cases (Tier-1 data, negative
  codes, `--package-map`, nullable/optional styles, roles).
- Spec alignment with the reference implementation (2026-09-06):
  package/world/interface segments mangle reserved words (§3.1); an
  interface declaration whose name is repeated by a member type mangles per
  §4.3 (`interface error { resource error; }` → `Error_`) (§6); inline world
  interfaces fold functions into aggregates and emit types into the world
  segment (§7.2); Javadoc `*/` escaping and no-invented-filler rule (§9);
  §8.1 normative support sources updated to the emitted bytes; §4.2 group-2
  mangling scoped to lowerCamel positions.
- Second hardening round (2026-09-06): new conformance case `empty-interface`
  pins §2/§6 — an interface with no members (or with everything filtered out
  by disabled feature gates) still emits its declaration file, world-referenced
  or not. `feature-gates` and `feature-gates-default` pin §2 both ways
  (`--features` enables `@unstable(feature)` items; the default skip is
  silent, not a diagnostic). `world-single-role` pins §7.1 (with `--role
  guest` only the guest aggregates are generated and the role segment stays).
  The suite is now 18 positive + 7 negative cases. The `option-nullable`
  expected tree now carries the support `Nullable` import (the reference
  implementation previously emitted `@Nullable` unresolvable; Tier-1 digests
  are unaffected — imports are not compared, §14).
- error-codes.md documents that WJ0005 is a defensive backstop: upstream
  resolution rejects duplicate identifiers in every scope and WIT identifiers
  cannot contain `_`, so no conforming input can trigger it — no negative
  case exists. §5.6's record-mangling wording clarified (mangling scope is
  the single record).
