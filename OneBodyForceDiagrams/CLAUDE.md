# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a physics education application for practicing one-body force diagrams (free-body diagrams). Students draw force vectors on objects in various physical scenarios and receive feedback on correctness. The project exists in three implementations:

- **OneBodyForceDiagrams-Java** — Original Java applet version (includes compiled `.class` files), authored by Peter Signell (1997, updated 2008)
- **OneBodyForceDiagramsJavaClassless** — Same Java source without compiled classes
- **OneBodyForceDiagrams-HTML5** — HTML5/Canvas rewrite (in progress), using layered `<canvas>` elements for vector drawing with mouse interaction

## Java Architecture

The Java version is a `java.applet.Applet` with this structure:

- **`ForcesMaster`** — Main applet entry point. Reads HTML parameters (`problemNo`, `weight`, `angle`, `acceleration`, `maxTriesEachForce`, `problemMode`) and wires together the four canvas panels.
- **`GeneralData`** — Interface with shared constants (colors, dimensions, layout positions) used across all classes.
- **`ProblemData`** — Interface that all problem types must implement (force counts, true answers, drawing methods).
- **`ProblemSelector`** — Factory that maps `problemNo` (0–11) to a specific problem class.
- **Problem classes** — Each scenario implements `ProblemData`: `HangingBall`, `HangingBall0`, `PersonHorizS`, `PersonHorizD`, `PersonHorizD2`, `BoxInclineS`, `BoxInclineS2`, `PersonIncliS`, `PersonIncliD`, `PulleyHorizSB`, `PulleyHorizSP`, `PulleyHorizSP4`.
- **UI panels**: `ProblemCanvas` (shows the physical setup), `UserCanvas` (where student draws vectors), `ResultsCanvas` (shows correctly drawn forces), `MessageCanvas` (instructions/feedback).
- **Helpers**: `CanvasVector` (vector drawing), `CanvasEquation` (equation rendering), `MessageDisplay`, `Forceable`.

Problems are launched from HTML pages (`Problem0.html`–`Problem11.html`) that embed the applet with `<PARAM>` tags.

## HTML5 Version

Located in `OneBodyForceDiagrams-HTML5/`. Uses stacked transparent `<canvas>` elements (z-indexed) for background, data display, and individual force vectors. Drawing is done via mouse events (`onmousedown`/`onmousemove`). Files are standalone HTML with inline `<script>` — no build system. Open directly in a browser to test.

## Building the Java Version

```bash
# From within either Java directory (requires JDK with applet support, e.g., JDK 8):
javac -classpath . ForcesMaster.java
```

The code depends on `gjt.*` (Graphic Java Toolkit) and `corejava.Console` — these libraries must be on the classpath.

## Adding a New Problem

1. Create a new class implementing `ProblemData` (follow existing patterns like `HangingBall.java`).
2. Register it in `ProblemSelector` constructor with a new `problemNo`.
3. Create a corresponding `ProblemN.html` with appropriate `<PARAM>` values.
