import { ScrollView, Text, View } from "react-native";
import { Button, Heading } from "@/components/ui";
import { SessionStatus } from "@/components/session-status";
import { useSession } from "@/lib/auth/use-session";
import { fonts, useTheme } from "@/theme/theme";
import { useRecognition } from "./use-recognition";
import { sampleDurationSeconds } from "./recognition-session";
import { RecognitionResult } from "./recognition-result";
import { RecognitionError } from "./recognition-error";
import { IdentifyButton } from "./identify-button";

const headings = {
  idle: "Discover the music",
  preparing: "Getting ready",
  recording: "Listening…",
  processing: "Finding your song",
  canceling: "Canceling",
  success: "Your discovery",
  error: "Discover the music",
} as const;

export default function ListenScreen() {
  const theme = useTheme();
  const auth = useSession();
  const recognition = useRecognition();
  const status = recognition.state.status;
  const recording = status === "recording";
  const busy =
    status === "preparing" || status === "processing" || status === "canceling";

  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={{
        padding: 24,
        gap: 24,
        paddingBottom: 40,
        flexGrow: 1,
      }}
      style={{ backgroundColor: theme.canvas }}
    >
      <View style={{ flex: 1, justifyContent: "center", gap: 28 }}>
        <Heading
          title={headings[status]}
          description="Hear something you love? Capture a short clip and find it in the HashTune library."
        />
        <View style={{ alignItems: "center", gap: 24, paddingVertical: 20 }}>
          <IdentifyButton
            status={status}
            disabled={busy || !auth.data}
            onPress={() => {
              if (recording) void recognition.finish();
              else void recognition.start();
            }}
          />
          <Text
            selectable
            accessibilityLiveRegion="polite"
            style={{
              fontFamily: fonts.medium,
              color: theme.muted,
              fontSize: 14,
              fontVariant: ["tabular-nums"],
            }}
          >
            {recording
              ? `${Math.min(sampleDurationSeconds, Math.floor(recognition.durationMs / 1000))} / ${sampleDurationSeconds} seconds`
              : "No account needed. Just music."}
          </Text>
          <Text
            selectable
            style={{
              fontFamily: fonts.regular,
              fontSize: 12,
              lineHeight: 18,
              color: theme.muted,
              textAlign: "center",
            }}
          >
            A short recording is sent to HashTune and saved in your history.
          </Text>
          {(recording || busy) && (
            <Button
              secondary
              label="Cancel"
              disabled={status === "canceling"}
              onPress={() => {
                void recognition.cancel();
              }}
            />
          )}
        </View>
        <SessionStatus />
        {recognition.state.status === "error" && (
          <RecognitionError error={recognition.state.error} />
        )}
        {recognition.state.status === "success" && (
          <RecognitionResult result={recognition.state.result} />
        )}
      </View>
    </ScrollView>
  );
}
