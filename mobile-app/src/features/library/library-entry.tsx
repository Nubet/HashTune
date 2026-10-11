import { Pressable, Text, View } from "react-native";
import { ChevronRight } from "lucide-react-native";
import { Artwork } from "@/components/artwork";
import { TrackRow } from "@/components/track-row";
import { fonts, useTheme } from "@/theme/theme";
import type { LibraryEntry } from "./use-library";

export function LibraryEntryRow({
  entry,
  onAlbum,
  onArtist,
}: {
  entry: LibraryEntry;
  onAlbum: (title: string, artist: string) => void;
  onArtist: (name: string) => void;
}) {
  const theme = useTheme();
  if (entry.kind === "track")
    return (
      <View>
        <TrackRow track={entry.track} />
        <Text
          selectable
          style={{
            fontFamily: fonts.regular,
            fontSize: 12,
            color: theme.muted,
            paddingBottom: 16,
          }}
        >
          {entry.track.origin === "HASH_TUNE" ? "HashTune" : "MTG-Jamendo"}
          {entry.track.status !== "INDEXED" ? " · Not yet indexed" : ""}
        </Text>
      </View>
    );
  const title = entry.kind === "album" ? entry.album.title : entry.artist.name;
  const description =
    entry.kind === "album"
      ? `${entry.album.artist} · ${entry.album.trackCount} tracks`
      : `${entry.artist.trackCount} tracks · ${entry.artist.albumCount} albums`;
  const uri =
    entry.kind === "album" ? entry.album.coverArtUrl : entry.artist.imageUrl;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Browse ${title}, ${description}`}
      onPress={() => {
        if (entry.kind === "album")
          onAlbum(entry.album.title, entry.album.artist);
        else onArtist(entry.artist.name);
      }}
      style={({ pressed }) => ({
        flexDirection: "row",
        alignItems: "center",
        gap: 14,
        paddingVertical: 16,
        opacity: pressed ? 0.7 : 1,
      })}
    >
      <Artwork uri={uri} />
      <View style={{ flex: 1, gap: 5 }}>
        <Text
          style={{ fontFamily: fonts.semibold, fontSize: 16, color: theme.ink }}
        >
          {title}
        </Text>
        <Text
          style={{
            fontFamily: fonts.regular,
            fontSize: 13,
            lineHeight: 20,
            color: theme.muted,
          }}
        >
          {description}
        </Text>
      </View>
      <ChevronRight size={18} color={theme.muted} />
    </Pressable>
  );
}
