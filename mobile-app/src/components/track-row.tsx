import { Text, View } from "react-native";
import type { Track } from "@/lib/api/contracts";
import { fonts, useTheme } from "@/theme/theme";
import { Artwork } from "./artwork";

export function TrackRow({ track }: { track: Track }) {
  const theme = useTheme();
  return (
    <View
      style={{
        flexDirection: "row",
        gap: 14,
        alignItems: "center",
        paddingVertical: 14,
      }}
    >
      <Artwork uri={track.coverArtUrl} />
      <View style={{ flex: 1, gap: 4 }}>
        <Text
          selectable
          style={{ fontFamily: fonts.semibold, fontSize: 16, color: theme.ink }}
        >
          {track.title}
        </Text>
        <Text
          selectable
          style={{
            fontFamily: fonts.regular,
            fontSize: 14,
            color: theme.muted,
          }}
        >
          {track.artist}
        </Text>
        {track.album && (
          <Text
            selectable
            style={{
              fontFamily: fonts.regular,
              fontSize: 12,
              color: theme.muted,
            }}
          >
            {track.album}
          </Text>
        )}
      </View>
    </View>
  );
}
