import { useCallback } from "react";
import { Text, View } from "react-native";
import { useFocusEffect } from "expo-router";
import {
  setAudioModeAsync,
  useAudioPlayer,
  useAudioPlayerStatus,
} from "expo-audio";
import { useMutation } from "@tanstack/react-query";
import { Button, Message } from "@/components/ui";
import { getAccessToken } from "@/lib/auth/supabase";
import { musicApi } from "@/lib/api/music-api";
import { fonts, useTheme } from "@/theme/theme";

export function RecordingPlayer({ recognitionId }: { recognitionId: string }) {
  const theme = useTheme();
  const player = useAudioPlayer(null);
  const status = useAudioPlayerStatus(player);
  const play = useMutation({
    mutationFn: async () => {
      await setAudioModeAsync({
        playsInSilentMode: true,
        allowsRecording: false,
        shouldPlayInBackground: false,
      });
      if (!status.isLoaded || status.error) {
        const token = await getAccessToken();
        player.replace({
          uri: musicApi.recordingUrl(recognitionId),
          headers: { Authorization: `Bearer ${token}` },
        });
      } else if (
        status.didJustFinish ||
        status.currentTime >= status.duration
      ) {
        await player.seekTo(0);
      }
      player.play();
    },
  });

  useFocusEffect(
    useCallback(
      () => () => {
        player.pause();
      },
      [player],
    ),
  );

  return (
    <View style={{ gap: 10, paddingVertical: 12 }}>
      <Button
        secondary
        busy={play.isPending}
        label={status.playing ? "Pause recording" : "Play recording"}
        onPress={() => {
          if (status.playing) player.pause();
          else play.mutate();
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
      {(play.error || status.error) && (
        <Message
          error
          title="Recording unavailable"
          description={
            play.error?.message ??
            "The recording could not be played. Check your connection and try again."
          }
        />
      )}
    </View>
  );
}
