import { describe, expect, it, vi } from "vitest";
import { themes } from "./theme";

vi.mock("react-native", () => ({ useColorScheme: () => "light" }));

describe("HashTune brand", () => {
  it("matches the frontend application tokens in both color schemes", () => {
    expect(themes.light).toMatchObject({
      primary: "#0A58CA",
      canvas: "#F7F8FB",
      ink: "#111827",
      line: "#DBE2EC",
    });
    expect(themes.dark).toMatchObject({
      primary: "#0A58CA",
      canvas: "#111114",
      ink: "#F7F7F8",
      line: "#2A2A31",
    });
  });
});
