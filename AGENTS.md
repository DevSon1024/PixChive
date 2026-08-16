# AGENTS.md - System Instructions for AI Agents

Welcome to the **PixChive** codebase. When assisting with this project, you must strictly adhere to the architectural boundaries, UI standards, and workflow rules defined in this document. 

## 1. Project Architecture & Separation of Concerns

PixChive is fundamentally divided into two distinct operational domains:
1. **Image Gallery**
2. **Comic Reading**

These domains operate with completely separate working environments, logic, and data handling.
- **Current Structure:** The gallery logic and UI are isolated within their own dedicated `gallery` package folder.
- **Future Structure:** A dedicated `comic` package will be established using the exact same isolated architecture. 
- **Rule:** Never bleed logic, ViewModels, or state management between the Gallery and Comic Reader domains. Keep their ecosystems completely encapsulated.

## 2. UI / Jetpack Compose Guidelines

- **Framework:** Jetpack Compose is the primary UI framework. Avoid legacy XML layouts entirely.
- **Material Design:** Strictly utilize Material Design 3 (M3) components and styling (`androidx.compose.material3.*`) to ensure a clean, native, and smooth experience.
- **Composables:** Keep composable functions highly focused and modular. Extract reusable UI elements into the appropriate `components` packages.
- **State Hoisting:** Prefer state hoisting for UI components to keep them stateless and reusable where appropriate. Do not perform heavy O(N) calculations or file operations directly within composable functions.

## 3. Code Quality & Performance Optimization

- **Language:** Kotlin is the exclusive programming language.
- **Asynchronous Operations:** Use Kotlin Coroutines and Flows for all asynchronous programming and state observation. Avoid RxJava or traditional callback interfaces.
- **Null Safety:** Handle nullable types safely. **Never** use the not-null assertion operator (`!!`) unless absolutely and undeniably necessary (and accompanied by an explanatory comment).
- **Performance:** Prioritize lag-free, 60/120fps performance. Avoid memory thrashing by efficiently caching instances (like formatters) and using optimized data loading strategies (like Paging 3) for large media collections.

## 4. Documentation & Update Tracking

You must actively maintain the project's changelog. After every completed task, error resolution, or feature addition, you must append an entry to the `update_details.md` file.

**Format for `update_details.md` entries:**
- **Type of Details:** (e.g., Error Solving, New Update, Refactor, Performance Improvement, More)
- **Description:** A brief, clear summary of what was changed, fixed, or added in the recent chat/interaction.
- **End Marker:** After the details of the latest update, you must append `---` to close out that specific chat area/session.

### Example Update Entry:
**Type:** Error Solving
**Details:** Migrated `getAllImages` to use Paging 3 to resolve OOM exceptions and heavy GC pauses. Refactored `GalleryImageItem` to optimize Coil `ImageRequest` for smoother high-column grid scrolling.
---