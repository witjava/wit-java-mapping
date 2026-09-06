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
