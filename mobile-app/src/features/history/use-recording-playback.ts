import { useCallback, useEffect, useRef, useState } from "react";
import { AppState } from "react-native";
import { useFocusEffect } from "expo-router";
import { File, Paths } from "expo-file-system";
import {
  setAudioModeAsync,
  useAudioPlayer,
  useAudioPlayerStatus,
} from "expo-audio";
import { musicApi } from "../../lib/api/music-api";

type PlaybackState =
  { status: "ready" | "loading" } | { status: "error"; error: Error };

export function useRecordingPlayback(recognitionId: string) {
  const player = useAudioPlayer(null);
  const audio = useAudioPlayerStatus(player);
  const [state, setState] = useState<PlaybackState>({ status: "ready" });
  const pending = useRef<AbortController | null>(null);
  const cachedRecording = useRef<File | null>(null);

  const stop = useCallback(() => {
    pending.current?.abort();
    player.pause();
    setState({ status: "ready" });
  }, [player]);
  useFocusEffect(useCallback(() => stop, [stop]));
  useEffect(() => {
    const subscription = AppState.addEventListener("change", (state) => {
      if (state === "background") stop();
    });
    return () => {
      subscription.remove();
      pending.current?.abort();
      if (cachedRecording.current?.exists) cachedRecording.current.delete();
    };
  }, [stop]);

  async function toggle() {
    if (audio.playing) {
      player.pause();
      return;
    }
    pending.current?.abort();
    const operation = new AbortController();
    pending.current = operation;
    setState({ status: "loading" });
    try {
      await setAudioModeAsync({
        playsInSilentMode: true,
        allowsRecording: false,
        shouldPlayInBackground: false,
      });
      if (operation.signal.aborted) return;
      if (!audio.isLoaded || audio.error) {
        if (!cachedRecording.current?.exists) {
          const bytes = await musicApi.recording(
            recognitionId,
            operation.signal,
          );
          if (operation.signal.aborted) return;
          const file = new File(
            Paths.cache,
            `hashtune-recording-${Date.now()}.m4a`,
          );
          cachedRecording.current = file;
          file.write(new Uint8Array(bytes));
        }
        player.replace({ uri: cachedRecording.current.uri });
      } else if (audio.didJustFinish || audio.currentTime >= audio.duration) {
        await player.seekTo(0);
      }
      if (operation.signal.aborted) return;
      player.play();
      setState({ status: "ready" });
    } catch (error) {
      if (!operation.signal.aborted)
        setState({
          status: "error",
          error:
            error instanceof Error
              ? error
              : new Error("The recording could not be played."),
        });
    }
  }

  return { audio, state, toggle };
}
