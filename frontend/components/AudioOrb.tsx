import { OrbBloop } from "@/components/orb/bloop";
import { BloopState } from "@/components/orb/bloop/types";
import { BLOOP_PALETTES, BloopPaletteName } from "@/components/orb/bloop/palettes";
import { useTheme } from "@/lib/theme";

type AudioOrbProps = {
  audioStream: MediaStream;
};

export function AudioOrb({ audioStream }: AudioOrbProps) {
  const theme = useTheme();
  const isLight = theme === "light";
  const palette = BLOOP_PALETTES[isLight ? BloopPaletteName.light : BloopPaletteName.dark];

  return (
    <OrbBloop
      size={288}
      audioMode="mic"
      audioStream={audioStream}
      state={BloopState.listen}
      className={
        isLight
          ? "saturate-[1.6] contrast-[1.12] drop-shadow-[0_1.5rem_1.75rem_rgba(180,83,9,0.3)]"
          : "saturate-[1.35] contrast-[1.16] drop-shadow-[0_1.5rem_1.75rem_rgba(0,112,255,0.35)]"
      }
      bloopColorMain={palette.main}
      bloopColorLow={palette.low}
      bloopColorMid={palette.mid}
      bloopColorHigh={palette.high}
      watercolorStrength={isLight ? 0.35 : 0.4}
      watercolorAnimated={false}
    />
  );
}
