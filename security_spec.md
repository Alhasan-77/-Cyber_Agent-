# Security Specification — NovaForge AI Studio

## 1. Data Invariants
1. **Strict Multi-Tenant Isolation**: Every document in `/users/{userId}/projects/{projectId}`, `/users/{userId}/snapshots/{snapshotId}`, `/users/{userId}/collections/{collectionId}`, and `/users/{userId}/logs/{logId}` must belong exclusively to `userId == request.auth.uid` and have `data.userId == request.auth.uid`.
2. **Immutable Metadata**: `userId` and `createdAt` (and `projectId` on child entities) can never be mutated during an `update`.
3. **Temporal Integrity**: All `createdAt` and `updatedAt` fields must be server-generated Firestore `timestamp` objects satisfying `<= request.time`.
4. **Strict Key & Size Enforcement**: Every `create` validates required and allowed keys via `hasAll` and `hasOnly`. Every `update` validates `isValid[Entity](incoming())` first and restricts modified keys via `incoming().diff(existing()).affectedKeys().hasOnly(...)`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/projects").where("userId", "==", "alice").get()` -> Reject (`request.auth != null`).
2. **Cross-User Read**: `bobDb.doc("users/alice/projects/p1").get()` -> Reject (`request.auth.uid == userId`).
3. **Unfiltered List Scraping**: `aliceDb.collection("users/alice/projects").get()` (missing `.where("userId", "==", "alice")`) -> Reject (`resource.data.userId == request.auth.uid`).
4. **Identity Spoofing on Create**: `aliceDb.doc("users/alice/projects/p1").set({ userId: "bob", ... })` -> Reject (`incoming().userId == request.auth.uid`).
5. **Shadow Field Injection on Create**: Adding `{ isVerifiedAdmin: true }` -> Reject (`incoming().keys().hasOnly(...)`).
6. **Shadow Field Injection on Update**: Updating with `{ role: "admin" }` -> Reject (`affectedKeys().hasOnly(...)`).
7. **Immutable Field Tampering**: Updating `createdAt` or `userId` on an existing project -> Reject (`incoming().createdAt == existing().createdAt`).
8. **Value Poisoning on Update**: Updating `name` with a number `12345` or empty string `""` -> Reject (`isValidStudioProject(incoming())`).
9. **Oversized String DoS**: Writing a 100KB `description` string -> Reject (`data.description.size() <= 1000`).
10. **Future Timestamp Spoofing**: Writing a timestamp in the year 2099 -> Reject (`incoming().createdAt <= request.time`).
11. **Append-Only Snapshot Mutation**: Attempting to update an immutable `VersionSnapshot` -> Reject (`allow update: if false;`).
12. **Invalid Enum Status in Build Logs**: Writing `status: "HACKED"` to `/users/alice/logs/l1` -> Reject (`data.status in ['SUCCESS', 'WARNING', 'ERROR', 'RUNNING']`).
