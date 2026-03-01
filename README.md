# Image Binner

Image Binner is a manual photo sorting app for Android.

> [!WARNING]
> This application is vibe coded with GPT-Codex-5.3.

## Usage

### Full photo access is required

- The app requires full library read access before main tabs are shown.
- On Android 14+ (API 34+), grant full access (not "selected photos only") so sorting can read every photo in the chosen source album.

### Profile model

- A profile stores:
  - one source album
  - an ordered list of destination albums (bins)
- Destination edits are auto-saved.
- The app keeps one global saved sorting session across all profiles.
- Instrumented coverage currently checks the resume/discard prompt from the profiles route (`SessionResumeFlowTest`); full app flow still requires connected-device runs.

### Trash behavior by API level

- API 24-29: trash album path only.
- API 30+: choose either:
  - system trash (`MediaStore.createTrashRequest` path), or
  - trash album path.
- Settings exposes the available option set per API level.

## v1 limits

- Photos only (no video workflow).
- No undo history after assignment/commit actions.
- One global session at a time.
- No multi-destination assignment per photo.

# License

MIT
