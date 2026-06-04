# MEvento v2 Core Conformance

This folder contains the executable baseline for MEvento v2 behavior.

`v2_core.tsv` is intentionally line-oriented so every host implementation can
read it without adding parser dependencies. Each non-comment row has four
tab-separated fields:

```text
id<TAB>expected_type<TAB>expected_value<TAB>source
```

The `source` and `expected_value` fields may use `\n`, `\r`, `\t`, and `\\`
escapes.

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
- Host functions receive already evaluated arguments.
- English, French, and Bambara keyword dictionaries must preserve equivalent
  behavior.

Future v2 changes should add rows here first, then update each implementation
until the same corpus passes everywhere.
