import { memo } from "react";
import { Text, View } from "react-native";
import type { HistoryItem } from "@/lib/api/contracts";
import { Button, Card, Message } from "@/components/ui";
import { TrackRow } from "@/components/track-row";
import { fonts, useTheme } from "@/theme/theme";
import { RecordingPlayer } from "./recording-player";

export const HistoryEntry = memo(function HistoryEntry({
  item,
  active,
  disabled,
  onPlay,
}: {
  item: HistoryItem;
  active: boolean;
  disabled: boolean;
  onPlay: (id: string) => void;
}) {
  const theme = useTheme();
  return (
    <Card>
      {item.track ? (
        <TrackRow track={item.track} />
      ) : (
        <Message
          title={
            item.status === "INVALID_AUDIO"
              ? "Audio could not be read"
              : "Nothing matched"
          }
          description="No track was identified in this search."
        />
      )}
      <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
        <Text
          selectable
          style={{
            fontFamily: fonts.medium,
            fontSize: 12,
            color: theme.primaryText,
          }}
        >
          {item.status === "MATCHED"
            ? "Match found"
            : item.status === "INVALID_AUDIO"
              ? "Invalid audio"
              : "No match"}
        </Text>
        <Text
          selectable
          style={{
            fontFamily: fonts.regular,
            fontSize: 12,
            color: theme.muted,
          }}
        >
          {item.source === "MICROPHONE" ? "Microphone" : "Audio file"}
        </Text>
        <Text
          selectable
          style={{
            fontFamily: fonts.regular,
            fontSize: 12,
            color: theme.muted,
          }}
        >
          {new Date(item.createdAt).toLocaleString("en-US", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })}
        </Text>
      </View>
      {item.source === "MICROPHONE" &&
        item.recordingUrl &&
        (active ? (
          <RecordingPlayer key={item.id} recognitionId={item.id} />
        ) : (
          <Button
            secondary
            label="Listen to sample"
            disabled={disabled}
            onPress={() => onPlay(item.id)}
          />
        ))}
    </Card>
  );
});
