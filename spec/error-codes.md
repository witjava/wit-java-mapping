# Error code registry — mapping v1

Codes are frozen once published; new codes may only be **appended**.

Every diagnostic MUST carry the WIT source location and its code. A conformance
negative case asserts: non-zero exit + the expected code (never a message
substring — wording is not normative).

| code | slug | condition |
|---|---|---|
| WJ0001 | `unsupported-type-construct` | `future<T>` / `stream<T>` / `error-context` / fixed-size `list<T,N>` / `map<K,V>`; the message MUST name the construct |
| WJ0002 | `tuple-arity-exceeded` | tuple arity > 8 |
| WJ0003 | `flags-arity-exceeded` | flags member count > 64 |
| WJ0004 | `fqn-collision` | two WIT items map to the same Java FQN |
| WJ0005 | `mangling-collision` | a mangled name collides with an already-mapped name |
| WJ0006 | `nested-option-under-nullable` | nested `option` under `--option-style=nullable` |
| WJ0007 | `package-collision` | two WIT packages land in the same Java package (via `--package-map` or version-segment collision) with same-named items |

Note on WJ0005: it is a defensive backstop. Upstream resolution rejects
duplicate identifiers in every scope (package, interface, world, record,
parameter list), and WIT identifiers cannot contain `_`, so no conforming
input can preempt a mangled candidate or exhaust the append loop. The
conformance suite therefore has no negative case for WJ0005; the code stays
registered and frozen for implementations whose mangling scopes are wider.
