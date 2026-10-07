import { useState } from "react";
import { GenerativeVisual } from "@norbert-fila/react-generative-visual";
import { useTheme, type Theme } from "@/lib/theme";

const palettes: Record<Theme, string[][]> = {
  dark: [
    ["#063C42", "#0BA6A6", "#9BE564"],
    ["#101C52", "#3155D9", "#75A8FF"],
    ["#42133B", "#B92B8E", "#F45B69"],
    ["#4A210A", "#E35D19", "#FFD166"],
  ],
  light: [
    ["#08A6A6", "#78D64B", "#E8F28A"],
    ["#3155D9", "#7098FF", "#D7B8FF"],
    ["#C02688", "#F45B69", "#FFB86B"],
    ["#E35D19", "#FF9F1C", "#FFE066"],
  ],
};

function paletteFor(seed: string, theme: Theme) {
  const hash = [...seed].reduce((value, character) => value * 31 + character.charCodeAt(0), 0);
  return palettes[theme][Math.abs(hash) % palettes[theme].length];
}

export function TrackArtwork({
  src,
  alt,
  seed,
  className,
}: {
  src?: string;
  alt: string;
  seed: string;
  className: string;
}) {
  const [failed, setFailed] = useState(false);
  const theme = useTheme();
  const colors = paletteFor(seed, theme);

  if (!src || failed) {
    return (
      <div className={className} aria-label={alt} role="img">
        <GenerativeVisual
          seed={seed}
          colors={colors}
          width="100%"
          height="100%"
          className="h-full w-full"
          complexity={0.34}
          contrast={0.82}
          distortion={0.42}
          softness={0.78}
          texture={0.82}
          sourceCount={4}
          sourceSize={1.08}
          separation={0.14}
          blur={0.72}
          grainAmount={0.62}
          grainSize={0.48}
        />
      </div>
    );
  }

  return (
    <img
      className={`${className} object-cover`}
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
    />
  );
}
