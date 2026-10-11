import { useColorScheme } from "react-native";

const brand = {
  primary: "#0A58CA",
  blue: "#3B82F6",
  paleBlue: "#BFDBFE",
  white: "#FFFFFF",
  success: "#10B981",
  danger: "#DC2626",
} as const;

export const themes = {
  light: {
    ...brand,
    canvas: "#F7F8FB",
    surface: "#FFFFFF",
    subtle: "#EEF1F6",
    ink: "#111827",
    muted: "#64748B",
    line: "#DBE2EC",
    primaryText: "#0A58CA",
    danger: "#DC2626",
  },
  dark: {
    ...brand,
    canvas: "#111114",
    surface: "#17171B",
    subtle: "#1A1A1F",
    ink: "#F7F7F8",
    muted: "#96969F",
    line: "#2A2A31",
    primaryText: "#60A5FA",
    danger: "#F87171",
  },
};

export const fonts = {
  regular: "Geist_400Regular",
  medium: "Geist_500Medium",
  semibold: "Geist_600SemiBold",
  bold: "Geist_700Bold",
} as const;

export function useTheme() {
  const dark = useColorScheme() === "dark";
  return { ...themes[dark ? "dark" : "light"], dark };
}
