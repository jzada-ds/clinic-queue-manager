# Design and operation analysis

## Four coordinated indexes

| Structure | Key | Stored information |
| --- | --- | --- |
| Doctors | Doctor ID | Patient count, cached next patient, queue, workload-node reference |
| Patients | Patient ID | Doctor ID and arrival sequence |
| One queue per doctor | Arrival sequence | Reference to the patient node |
| Doctor workloads | (Patient count, Doctor ID) | Subtree doctor count and total patient load |

The workload key includes doctor ID so that doctors with equal workloads remain distinct. The queue's minimum arrival sequence identifies the next patient. Arrival order is FIFO, not medical urgency.

The generic `TwoThreeTree<K,V,A>` separates keys, values, and aggregate data. `TTData` defines empty, leaf, and combine operations. The workload index combines counts and sums; the other indexes use no aggregate payload.

## Patient arrival

1. Find the doctor and reject an existing patient ID.
2. Insert the patient into the patient index with a new arrival sequence.
3. Insert the sequence into the doctor's queue.
4. Increase the doctor's cached count.
5. Remove the old workload entry and insert the updated (count, ID) key.
6. Refresh the cached next-patient ID.

The workload key is removed and reinserted because mutating a key in place would break the tree's ordering.

## Serving a patient and early departure

Serving removes the minimum arrival-sequence node. Early departure first looks up a patient ID, then uses its doctor ID and arrival sequence to locate the queue node. Both paths remove the patient from the global index, update the workload index, and refresh cached doctor information.

Each queue has at most P entries, so its O(log queue-size) cost fits within O(log P). The combined updates therefore take O(log D + log P).

## Range statistics

Each workload subtree stores:
- Number of doctors.
- Sum of their patient counts.

A prefix query accumulates complete subtrees while following one boundary path. For inclusive range [a,b], the implementation subtracts the prefix below a from the prefix through b:

```text
range count = prefix(b).count - prefix(a - 1).count
range sum   = prefix(b).sum   - prefix(a - 1).sum
average     = range sum / range count
```

Keys use MAX_ID to include every doctor with a boundary workload. Integer.MIN_VALUE is handled separately to avoid overflowing a - 1. Integer division floors the nonnegative average; an empty range returns zero. A reversed range throws IllegalArgumentException.

Because the traversal follows logarithmic-height paths and reads cached aggregates for completed subtrees, each range query is O(log D), rather than O(D).

## Space and invariants

The clinic stores O(D) doctor/workload entries and O(P) patient/queue entries. Each doctor contributes constant queue initialization overhead, giving O(D + P) total space.

Important invariants:
- Each active patient has one global entry and one queue entry.
- Cached counts match queue sizes.
- Every doctor has exactly one workload entry matching that count.
- The cached next patient matches the queue minimum.
- All tree leaves remain at the same depth, and internal aggregates reflect their children.

The functional reference-model tests exercise the externally visible effects of these invariants. They do not directly inspect every structural invariant.

## Boundaries

The submission uses a global int arrival counter, with Integer.MAX_VALUE reserved as a queue sentinel. It assumes valid IDs lie between the supplied minimum and maximum sentinels. It is single-threaded and in-memory. These limits are retained rather than silently changing the academic implementation.
