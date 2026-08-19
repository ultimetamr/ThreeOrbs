# ThreeOrbs project guidance

## Current project

ThreeOrbs is a PICO Spatial SDK 0.13.3 Android/Kotlin volumetric app. The product surface is now a real ECS 3D scene: three fixed-size emissive sphere entities, a shared movable parent group, an interactable 3D grab ring, a 3D date wall, and a generated constellation. SpatialUI is reserved for text entry and task-management decisions.

The generated structure is intentionally kept close to the official `pico-cli project create --template volumetric` output so SDK updates and examples remain easy to compare.

## Key files

- `app/src/main/java/com/example/threeorbs/Main.kt`: declares the default volumetric window container and wraps its UI in `PicoTheme`.
- `app/src/main/java/com/example/threeorbs/spatial/ThreeOrbsScene.kt`: creates the 3D ECS scene, colliders, hover/press targeting, 0.8-second long-pinch state machine, group drag, stardust transfer, and constellation.
- `app/src/main/java/com/example/threeorbs/ui/today/components/ThreeOrbsComponents.kt`: SpatialUI control, setup, edit, replacement, archive, history, and undo panels.
- `app/src/main/java/com/example/threeorbs/data/repository/DataStoreThreeOrbsRepository.kt`: local 14-day state and 3D group-position persistence.
- `app/src/main/java/com/example/threeorbs/platform/SpatialApplication.kt`: launches the Spatial app graph.
- `app/src/main/java/com/example/threeorbs/platform/LaunchActivity.kt`: Android launcher activity.
- `app/src/main/AndroidManifest.xml`: expanded invisible volumetric clipping bounds, fixed scaling, auto-hidden caption, and no base panel/frame.
- `app/src/androidTest/java/com/example/threeorbs/ExampleInstrumentedTest.kt`: package and launch/liveness checks.

## Spatial SDK capabilities in use

- Volumetric `DefaultWindowContainer`
- Runtime `MeshResource` primitives, PBR/unlit materials, `ModelComponent`, and parent-relative transforms
- `CollisionComponent`, `InteractableComponent`, `HoverEffectComponent`, and targeted spatial pointer/drag gestures
- SpatialUI attachment labels and glass management panels
- Fixed world scaling and uniform volumetric resizing

## UI rule

All 2D UI must use SpatialUI components under `com.pico.spatial.ui.*` and every UI root must be wrapped in `PicoTheme`. Do not add Android Compose Material or Material3 dependencies, imports, themes, or components. Route colors and typography through `PicoTheme` roles.

## Interaction contract

- Short pinch a 3D task sphere: edit that slot.
- Hold pinch for at least 800 ms: complete the active task and expose undo for 2 seconds.
- Grab and drag the 3D ring below the constellation: move one parent entity; child spacing and scale remain fixed.
- No rigid body, gravity, or collision response is used; colliders exist only for input targeting.

## Build, install, and run

From the project root on Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
pico-cli app launch com.example.threeorbs --activity .platform.LaunchActivity
```

Run the connected checks with:

```powershell
.\gradlew.bat connectedAndroidTest
```
