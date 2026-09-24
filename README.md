# Clinic Queue Manager — Augmented 2–3 Trees

A two-person academic Java project for managing doctors, FIFO patient queues, early departures, and workload-range statistics under explicit time and space constraints.

## Contributors

- **Jonathan Zada:** primarily algorithmic thinking and data-structure selection. Chose 2–3 trees to satisfy the assignment's time and space constraints.
- **Guy:** primarily implemented the Java code. Credited by first name.

The implementation is joint coursework; this repository does not claim that Jonathan independently wrote the code. The reference-model test harness and repository documentation were added during later portfolio preparation, with AI assistance, and were not part of the original submission.

## Why 2–3 trees?

The interface needs ordered operations with logarithmic bounds, including counting doctors by workload. A balanced 2–3 tree maintains logarithmic height through splits on insertion and borrowing or merging on deletion.

The same generic implementation is reused for ID lookup, arrival-order queues, and an augmented workload index. Subtree counts and sums make workload queries possible without scanning all doctors. A plain queue would preserve FIFO order, but would not by itself solve patient lookup, arbitrary early departure, and workload-range aggregation.

## Repository layout

- `src/ClinicManager.java`: clinic operations and coordinated indexes.
- `src/TwoThreeTree.java`: generic balanced tree and aggregation interface.
- `tests/ReviewCheck.java`: independent deterministic reference-model checks.
- `docs/design.md`: indexes, update paths, complexity, and limitations.

The two files in `src/` preserve the submitted implementation. The original assignment PDF and print-based course test driver are not included; the problem is summarized here in our own words.

## Run the checks

Requires a JDK, version 17 or later. No third-party libraries or build tools are needed.

```sh
javac -d build src/ClinicManager.java src/TwoThreeTree.java tests/ReviewCheck.java
java -cp build ReviewCheck
```

Expected output:

```text
PASS: 10 seeds x 3000 operations; 3983645 reference-model checks.
```

The harness compares the implementation with a simpler model using standard Java collections. It covers FIFO service, early departures, doctor insertion/removal, duplicate doctor rejection, patient ownership, queue sizes, workload ranges, and draining back to an empty state. Collections are used only in the later test harness, not in the submitted implementation.

These are functional checks, not a runtime benchmark, exhaustive proof, or the official grading results.

## Complexity

Let **D** be the number of active doctors and **P** the number of waiting patients.

| Operation | Time |
| --- | --- |
| Initialize | O(1) |
| Add/remove doctor | O(log D) |
| Add, serve, or remove a patient early | O(log D + log P) |
| Queue size / next patient for a doctor | O(log D) |
| Find a patient's doctor | O(log P) |
| Count doctors or average workload within a range | O(log D) |

Total storage is **O(D + P)**. Bounds assume the balanced-tree invariants, constant-time key comparisons under the assignment model, and counters within their supported integer range. String comparison cost also depends on ID length in a general setting.

## Scope

This is an in-memory data-structures exercise, not production clinical software. There is no persistence, authentication, concurrency control, or real patient data. The arrival sequence uses an integer counter and reserves an upper sentinel; a long-running application would need sequence-exhaustion handling. See [design notes](docs/design.md).
