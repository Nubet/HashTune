import { useCallback, useEffect, useMemo, useSyncExternalStore } from "react";
import { AppState } from "react-native";
import { useFocusEffect } from "expo-router";
import {
  requestRecordingPermissionsAsync,
  RecordingPresets,
  setAudioModeAsync,
  useAudioRecorder,
  useAudioRecorderState,
} from "expo-audio";
import { File } from "expo-file-system";
import { useQueryClient } from "@tanstack/react-query";
import { musicApi } from "@/lib/api/music-api";
import { getAccessToken } from "@/lib/auth/supabase";
import { RecognitionSession } from "./recognition-session";

export class MicrophonePermissionError extends Error {
  constructor(readonly canAskAgain: boolean) {
    super(
      canAskAgain
        ? "Microphone access is needed to identify music. Tap Identify to try again."
        : "Microphone access is disabled. Enable it in your phone settings to identify music.",
    );
  }
}

export function useRecognition() {
  const queryClient = useQueryClient();
  const recorder = useAudioRecorder({
    ...RecordingPresets.HIGH_QUALITY,
    isMeteringEnabled: true,
  });
  const recorderState = useAudioRecorderState(recorder, 200);
  const session = useMemo(
    () =>
      new RecognitionSession({
        async prepare(signal) {
          await getAccessToken();
          if (signal.aborted) return;
          const permission = await requestRecordingPermissionsAsync();
          if (!permission.granted)
            throw new MicrophonePermissionError(permission.canAskAgain);
          if (signal.aborted) return;
          await setAudioModeAsync({
            allowsRecording: true,
            playsInSilentMode: true,
            shouldPlayInBackground: false,
          });
          if (signal.aborted) return;
          await recorder.prepareToRecordAsync();
        },
        record: (forDuration) => recorder.record({ forDuration }),
        async stop() {
          if (recorder.isRecording) await recorder.stop();
        },
        uri: () => recorder.uri,
        async discard() {
          if (recorder.uri) {
            const file = new File(recorder.uri);
            if (file.exists) file.delete();
          }
          await setAudioModeAsync({
            allowsRecording: false,
            playsInSilentMode: true,
          });
        },
        async recognize(uri, signal) {
          const file = new File(uri);
          if (!file.exists || file.size === 0)
            throw new Error(
              "The sample is empty. Record a longer clip and try again.",
            );
          return musicApi.recognize(file, signal);
        },
        onRecognized: () => {
          void queryClient.invalidateQueries({ queryKey: ["history"] });
        },
      }),
    [recorder, queryClient],
  );

  useEffect(() => {
    const appState = AppState.addEventListener("change", (state) => {
      if (state === "background")
        void session.cancel(
          "Recording was interrupted. Keep HashTune open and try again.",
        );
    });
    const recording = recorder.addListener(
      "recordingStatusUpdate",
      (status) => {
        if (status.hasError || status.mediaServicesDidReset)
          void session.cancel(
            "The microphone was interrupted. Please try again.",
          );
        else if (status.isFinished) void session.finish();
      },
    );
    return () => {
      appState.remove();
      recording.remove();
      void session.cancel();
    };
  }, [session, recorder]);

  useFocusEffect(
    useCallback(
      () => () => {
        void session.cancel();
      },
      [session],
    ),
  );

  const state = useSyncExternalStore(
    session.subscribe,
    session.getSnapshot,
    session.getSnapshot,
  );
  return {
    state,
    durationMs: recorderState.durationMillis,
    start: () => session.start(),
    finish: () => session.finish(),
    cancel: () => session.cancel(),
  };
}
