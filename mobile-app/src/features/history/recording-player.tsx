import { Text, View } from "react-native";
import { Button, Message } from "@/components/ui";
import { fonts, useTheme } from "@/theme/theme";
import { useRecordingPlayback } from "./use-recording-playback";

export function RecordingPlayer({ recognitionId }: { recognitionId: string }) {
  const theme = useTheme();
  const { audio: status, state, toggle } = useRecordingPlayback(recognitionId);

  return (
    <View style={{ gap: 10, paddingVertical: 12 }}>
      <Button
        secondary
        busy={state.status === "loading"}
        label={status.playing ? "Pause recording" : "Play recording"}
        onPress={() => {
          void toggle();
        }}
      />
      {status.isBuffering && (
        <Text style={{ color: theme.muted, fontFamily: fonts.regular }}>
          Loading recording…
        </Text>
      )}
      {status.isLoaded && (
        <Text
          selectable
          style={{
            fontFamily: fonts.regular,
            color: theme.muted,
            fontSize: 12,
            fontVariant: ["tabular-nums"],
          }}
        >
          {Math.floor(status.currentTime)} / {Math.ceil(status.duration)}{" "}
          seconds
        </Text>
      )}
      {(state.status === "error" || status.error) && (
        <Message
          error
          title="Recording unavailable"
          description={
            state.status === "error"
              ? state.error.message
              : "The recording could not be played. Check your connection and try again."
          }
        />
      )}
    </View>
  );
}
