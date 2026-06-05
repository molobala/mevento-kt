# MEvento v2 Core Conformance

This folder contains the executable baseline for MEvento v2 behavior and v1
script compatibility.

`v1_compat.tsv` and `v2_core.tsv` are intentionally line-oriented so every host
implementation can read them without adding parser dependencies. Each
non-comment row has four tab-separated fields:

```text
id<TAB>expected_type<TAB>expected_value<TAB>source
```

The `source` and `expected_value` fields may use `\n`, `\r`, `\t`, and `\\`
escapes.

`v1_compat.tsv` contains scripts written only with v1-era syntax. It is the
migration guard for old database scripts and must not include v2-only syntax or
built-ins such as dot property access, `_try_`, trace, manifests, or result
helpers.

One v1 behavior is intentionally not preserved there: old missing host function
calls returned `null`, while v2 raises `unknown_function` unless the call is
wrapped in `_try_`.

## Core Contract Covered

- `null`, `false`, numeric zero, and empty strings are falsey.
- All other values are truthy.
- `&&` and `||` return boolean results using MEvento truthiness.
- `??` returns the left value when it is not `null`; otherwise it evaluates and
  returns the right value.
- Numeric `for` loops are inclusive of the `till` boundary.
- Block scope can update variables already declared in a parent scope, while
  new variables declared inside the block remain local.
- Arrays, maps, index access, and index assignment use host-native collection
  values.
- Built-in collection helpers cover array length, push, pop, insert,
  remove-at, plus map has/keys/values.
- Host functions receive already evaluated arguments.
- `_try_(expression)` evaluates its single expression lazily and returns a
  result map: `{ok: true, value: ..., error: null}` or
  `{ok: false, value: null, error: {message, line, col, node}}`.
- English, French, and Bambara keyword dictionaries must preserve equivalent
  behavior.

Future v2 changes should add rows here first, then update each implementation
until the same corpus passes everywhere.

`v2_validation.tsv` covers preflight validation. Each non-comment row has five
tab-separated fields:

```text
id<TAB>known_functions<TAB>ok<TAB>expected_error_code<TAB>source
```

`known_functions` is a comma-separated capability list. Entries may be bare
names, or `name:min..max` when arity metadata is available. Use `*` for an
unbounded side of the range, and `-` when no specific error code is expected.
