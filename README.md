# NickezKeystrokes

Smooth, animated keystrokes HUD for **Fabric 1.21.11** (client-side only, needs Fabric API).

## Features
- Smooth eased press/release animation (+ optional press "pop")
- **W A S D** — show / hide
- **Space bar** — show / hide
- **LMB / RMB** buttons with live **CPS** (per-button, or a combined total)
- Sneak / Sprint bars
- Rainbow mode, custom colours, opacity, size, gap, scale, position
- Config saved to `config/nickezkeystrokes.json`

## Customization menu
Press **N** in game to open it (live animated preview + 4 tabs: Keys, Look, Layout, Controls).
Change the key in **Options → Controls → Key Binds → NickezKeystrokes**, or in the menu's **Controls** tab.

## In-game commands
`/nickez` lists everything. Examples:

```
/nickez wasd            (toggle)      /nickez wasd false
/nickez space           /nickez mouse        /nickez cps
/nickez totalcps        /nickez mods         /nickez rainbow
/nickez pos 10 10       /nickez scale 1.5    /nickez opacity 0.5
/nickez speed 30        /nickez color FF55AA /nickez reset
```

## Building with GitHub Actions
1. Create a GitHub repo and push this folder.
2. The workflow in `.github/workflows/build.yml` builds on every push; download the jar from the run's **Artifacts**.
3. To publish a release: `git tag v1.0.0 && git push --tags` — the jar is attached automatically.

## Building locally
Requires JDK 21 and Gradle 9.2+: `gradle build` → jar in `build/libs/`.
(Optional: run `gradle wrapper --gradle-version 9.2.1` once to generate `gradlew`.)
