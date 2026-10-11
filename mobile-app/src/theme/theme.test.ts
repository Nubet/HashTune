import { readFileSync } from "node:fs";
import { describe, expect, it, vi } from "vitest";
import { themes } from "./theme";

vi.mock("react-native", () => ({ useColorScheme: () => "light" }));

describe("HashTune brand", () => {
  it("matches the frontend application tokens in both color schemes", () => {
    const css = readFileSync(
      new URL("../../../frontend/app/globals.css", import.meta.url),
      "utf8",
    );
    const light = css.match(/\n\.app-shell \{([^}]+)}/)?.[1];
    const dark = css.match(/\n\.dark \.app-shell \{([^}]+)\}/)?.[1];
    expect(css).toContain(
      `--brand-primary-blue: ${themes.light.primary.toLowerCase()};`,
    );
    for (const [block, theme] of [
      [light, themes.light],
      [dark, themes.dark],
    ] as const) {
      expect(block).toContain(`--bg-canvas: ${theme.canvas.toLowerCase()};`);
      expect(block).toContain(`--text-ink: ${theme.ink.toLowerCase()};`);
      expect(block).toContain(`--border-line: ${theme.line.toLowerCase()};`);
    }
  });
});
