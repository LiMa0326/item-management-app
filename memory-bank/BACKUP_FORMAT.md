# memory-bank/BACKUP_FORMAT.md — Backup Package Format (V0→V2)

> Purpose: Define a **platform-neutral** backup package format used for:
> - V0: Local export/import (offline backup)
> - V1: Cloud backup/restore (upload/download the same package)
> - V2+: Cross-platform clients (Android/iOS/Web) sharing the same data format
>
> Principles:
> 1) **Offline-first**: export/import must work without cloud.
> 2) **Low cost**: photos optional; support thumbnail-only mode.
> 3) **Forward-compatible**: unknown fields must be ignored on import.
> 4) **Versioned**: every backup has a format version and schema version.
> 5) **Platform-neutral**: avoid Android/iOS-specific URIs in the package.

---

## 1. Package Type & Structure

A backup package is a **single archive** (recommended: `.zip`) with these entries:

```
backup.zip
  /manifest.json
  /data.json
  /photos/                      (optional)
    <photoId>.<ext>             (optional)
  /checksums.json               (optional)
```

- `manifest.json`: metadata about this backup (required)
- `data.json`: the actual inventory data (required)
- `photos/`: embedded photos (optional, depends on export mode)
- `checksums.json`: integrity checks (optional)

---

## 2. Export Modes (Cost Control)

`exportMode` controls whether photos are included:

- `metadata_only`  
  Only JSON files. Lowest size/cost. (Good for frequent backups)
- `thumbnails`  
  JSON + compressed thumbnails in `photos/`. Recommended default for cloud backup.
- `full`  
  JSON + original photos in `photos/`. Largest.

Rules:
- `exportMode` must be recorded in `manifest.json`.
- Import must succeed even if `photos/` is missing (i.e., `metadata_only`).

---

## 3. File: manifest.json (Required)

### 3.1 Required Fields
- `formatVersion` (string): backup format version, e.g. `"1.0"`
- `createdAt` (string): ISO-8601 timestamp, e.g. `"2026-03-01T10:22:11Z"`
- `exportMode` (string): `metadata_only | thumbnails | full`
- `app` (object): app identifier/build
- `stats` (object): counts for quick inspection

### 3.2 Optional Fields
- `device` (object): device info (not required; keep generic)
- `notes` (string): optional human notes
- `encryption` (object): if package is encrypted in the future

### 3.3 Example
```json
{
  "formatVersion": "1.0",
  "createdAt": "2026-03-01T10:22:11Z",
  "exportMode": "thumbnails",
  "app": {
    "name": "HomeInventory",
    "build": "0.1.0",
    "platform": "android"
  },
  "stats": {
    "categories": 3,
    "items": 128,
    "photos": 220
  }
}
```

---

## 4. File: data.json (Required)

### 4.1 Top-level Structure
```json
{
  "schemaVersion": "1.0",
  "exportedAt": "2026-03-01T10:22:11Z",
  "categories": [],
  "items": [],
  "itemPhotos": []
}
```

### 4.2 Entity: Category
Required:
- `id` (string)
- `name` (string)
- `sortOrder` (number)
- `isArchived` (boolean)
- `createdAt`, `updatedAt` (ISO-8601)

Example:
```json
{
  "id": "cat_electronics",
  "name": "Electronics",
  "sortOrder": 10,
  "isArchived": false,
  "createdAt": "2026-02-20T05:00:00Z",
  "updatedAt": "2026-03-01T09:00:00Z"
}
```

### 4.3 Entity: Item
Required:
- `id` (string)
- `categoryId` (string)
- `name` (string)
- `createdAt`, `updatedAt` (ISO-8601)

Optional (recommended):
- `purchaseDate` (YYYY-MM-DD)
- `purchasePrice` (number)
- `purchaseCurrency` (string, e.g. "USD")
- `purchasePlace` (string)
- `description` (string)
- `tags` (string[])
- `customAttributes` (object/map) — **key-value for extensibility**
- `deletedAt` (ISO-8601) — present if soft-deleted

Example:
```json
{
  "id": "item_7b4f7f3e",
  "categoryId": "cat_electronics",
  "name": "Sony WH-1000XM5",
  "purchaseDate": "2025-10-12",
  "purchasePrice": 329.99,
  "purchaseCurrency": "USD",
  "purchasePlace": "Amazon",
  "description": "Noise cancelling headphone for commute",
  "tags": ["commute", "audio"],
  "customAttributes": {
    "color": "Black",
    "serialNumber": "SN12345678"
  },
  "createdAt": "2026-02-21T02:11:00Z",
  "updatedAt": "2026-03-01T09:11:00Z"
}
```

### 4.4 Entity: ItemPhoto
This entity links an item to a photo included in the package (or not).

Required:
- `id` (string)
- `itemId` (string)
- `contentType` (string, e.g. "image/jpeg")
- `createdAt` (ISO-8601)

Optional:
- `fileName` (string): points to `photos/<fileName>` when photos are embedded
- `width`, `height` (number)
- `kind` (string): `thumbnail | full` (optional but useful)

Rules:
- If `exportMode = metadata_only`, `fileName` is typically omitted.
- If `exportMode = thumbnails|full`, and photos are embedded, `fileName` must exist.

Example:
```json
{
  "id": "photo_aa11bb22",
  "itemId": "item_7b4f7f3e",
  "contentType": "image/jpeg",
  "fileName": "photo_aa11bb22.jpg",
  "width": 1200,
  "height": 1600,
  "kind": "thumbnail",
  "createdAt": "2026-02-21T02:12:00Z"
}
```

---

## 5. Folder: photos/ (Optional)

### 5.1 Naming
Recommended deterministic naming:
- `<photoId>.<ext>`

Example:
```
photos/photo_aa11bb22.jpg
```

### 5.2 Thumbnail Guidance (for `thumbnails` mode)
To reduce storage/cloud cost:
- Long side max around ~1280px (implementation can choose)
- Strip EXIF metadata (privacy)
- Compress for size

(Exact numbers are implementation details; keep configurable.)

---

## 6. Import Behavior

### 6.1 V0 Required Import Mode
- `replace_all`: wipe local data then import everything from `data.json`

### 6.2 Future Import Mode (V2+)
- `merge_upsert` (optional enhancement):
  - Upsert by `id`
  - Use `updatedAt` (or versioning) to resolve conflicts
  - Respect `deletedAt`

---

## 7. Compatibility Rules (Very Important)

- Importers **must ignore unknown fields**.
- `formatVersion` and `schemaVersion`:
  - If versions are the same → normal import
  - If backup is newer → best-effort import known fields + warn user
- Never rename existing fields without supporting migration logic.
- `customAttributes` is the official extension mechanism (avoid adding lots of new top-level fields).

---

## 8. Relationship to Cloud Backup (V1)

Cloud backup should upload/download the **same zip package**.
- V1 default recommendation: `thumbnails` mode
- Cloud restore should simply download the zip and run the same import flow.

---

## 9. Minimal Example (metadata_only)

`manifest.json`
```json
{
  "formatVersion": "1.0",
  "createdAt": "2026-03-01T10:22:11Z",
  "exportMode": "metadata_only",
  "app": { "name": "HomeInventory", "build": "0.1.0", "platform": "android" },
  "stats": { "categories": 1, "items": 1, "photos": 0 }
}
```

`data.json`
```json
{
  "schemaVersion": "1.0",
  "exportedAt": "2026-03-01T10:22:11Z",
  "categories": [
    {
      "id": "cat_shoes",
      "name": "Shoes",
      "sortOrder": 20,
      "isArchived": false,
      "createdAt": "2026-03-01T10:00:00Z",
      "updatedAt": "2026-03-01T10:00:00Z"
    }
  ],
  "items": [
    {
      "id": "item_001",
      "categoryId": "cat_shoes",
      "name": "Nike Pegasus 40",
      "purchaseDate": "2025-09-02",
      "purchasePrice": 89.0,
      "purchaseCurrency": "USD",
      "purchasePlace": "Nike Store",
      "description": "Daily trainer",
      "tags": ["running"],
      "customAttributes": { "sizeUS": 10, "color": "White" },
      "createdAt": "2026-03-01T10:01:00Z",
      "updatedAt": "2026-03-01T10:01:00Z"
    }
  ],
  "itemPhotos": []
}
```

---
