export function hexToRgb(hex: string): [number, number, number] {
  const cleanHex = hex.replace("#", "");
  const r = parseInt(cleanHex.substring(0, 2), 16) / 255;
  const g = parseInt(cleanHex.substring(2, 4), 16) / 255;
  const b = parseInt(cleanHex.substring(4, 6), 16) / 255;
  return [r, g, b];
}

export type BloopPalette = {
  main: [number, number, number];
  low: [number, number, number];
  mid: [number, number, number];
  high: [number, number, number];
};

export const BloopPaletteName = {
  blue: "BLUE",
  light: "LIGHT",
  dark: "DARK",
} as const;

export type BloopPaletteName = (typeof BloopPaletteName)[keyof typeof BloopPaletteName];

export const BLOOP_PALETTES: Record<BloopPaletteName, BloopPalette> = {
  [BloopPaletteName.blue]: {
    main: hexToRgb("#DCF7FF"),
    low: hexToRgb("#0181FE"),
    mid: hexToRgb("#A4EFFF"),
    high: hexToRgb("#FFFDEF"),
  },
  [BloopPaletteName.light]: {
    main: hexToRgb("#FFF1D6"),
    low: hexToRgb("#E05A00"),
    mid: hexToRgb("#FFB020"),
    high: hexToRgb("#FFF8EA"),
  },
  [BloopPaletteName.dark]: {
    main: hexToRgb("#F3FBFF"),
    low: hexToRgb("#0047FF"),
    mid: hexToRgb("#00CFFF"),
    high: hexToRgb("#FFFFFF"),
  },
};
