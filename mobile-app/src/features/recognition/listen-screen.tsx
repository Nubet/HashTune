import {
  ActivityIndicator,
  Linking,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Mic, Square } from "lucide-react-native";
import Animated, {
  FadeIn,
  ReduceMotion,
  useAnimatedStyle,
  withRepeat,
  withTiming,
} from "react-native-reanimated";
import { Button, Card, Heading, Message } from "@/components/ui";
import { SessionStatus } from "@/components/session-status";
import { useSession } from "@/lib/auth/use-session";
import { fonts, useTheme } from "@/theme/theme";
import { MicrophonePermissionError, useRecognition } from "./use-recognition";
import { sampleDurationSeconds } from "./recognition-session";
import { RecognitionResult } from "./recognition-result";

export default function ListenScreen() {
  const theme = useTheme();
  const auth = useSession();
  const recognition = useRecognition();
  const status = recognition.state.status;
  const recording = status === "recording";
  const busy =
    status === "preparing" || status === "processing" || status === "canceling";
  const orbStyle = useAnimatedStyle(
    () => ({
      transform: [
        {
          scale: recording
            ? withRepeat(
                withTiming(1.08, {
                  duration: 900,
                  reduceMotion: ReduceMotion.System,
                }),
                -1,
                true,
                undefined,
                ReduceMotion.System,
              )
            : withTiming(1),
        },
      ],
    }),
    [recording],
  );

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
          title={
            recording
              ? "Listening…"
              : status === "processing"
                ? "Finding your song"
                : "Discover the music"
          }
          description="Hear something you love? Capture a short clip and find it in the HashTune library."
        />
        <View style={{ alignItems: "center", gap: 24, paddingVertical: 20 }}>
          <Animated.View
            style={[
              {
                width: 210,
                height: 210,
                borderRadius: 105,
                backgroundColor: theme.dark ? "#0A58CA20" : "#BFDBFE70",
                padding: 15,
              },
              orbStyle,
            ]}
          >
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={
                recording
                  ? "Stop recording and identify music"
                  : "Identify music"
              }
              accessibilityState={{ disabled: busy || !auth.data, busy }}
              disabled={busy || !auth.data}
              onPress={() => {
                if (recording) void recognition.finish();
                else void recognition.start();
              }}
              style={({ pressed }) => ({
                flex: 1,
                borderRadius: 90,
                overflow: "hidden",
                opacity: pressed || busy ? 0.75 : 1,
              })}
            >
              <LinearGradient
                colors={[theme.primary, theme.blue, "#06B6D4"]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={{
                  flex: 1,
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 12,
                }}
              >
                {busy ? (
                  <ActivityIndicator color={theme.white} size="large" />
                ) : recording ? (
                  <Square size={42} color={theme.white} fill={theme.white} />
                ) : (
                  <Mic size={52} color={theme.white} strokeWidth={1.7} />
                )}
                <Text
                  style={{
                    fontFamily: fonts.semibold,
                    color: theme.white,
                    fontSize: 16,
                  }}
                >
                  {recording
                    ? "Stop & identify"
                    : status === "processing"
                      ? "Identifying…"
                      : status === "preparing"
                        ? "Getting ready…"
                        : status === "canceling"
                          ? "Canceling…"
                          : "Identify"}
                </Text>
              </LinearGradient>
            </Pressable>
          </Animated.View>
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
          <Animated.View
            entering={FadeIn.duration(200).reduceMotion(ReduceMotion.System)}
          >
            <Card>
              <Message
                error
                title="Let's try that again"
                description={recognition.state.error.message}
              />
              {recognition.state.error instanceof MicrophonePermissionError &&
                !recognition.state.error.canAskAgain && (
                  <Button
                    label="Open Settings"
                    onPress={() => {
                      void Linking.openSettings();
                    }}
                  />
                )}
            </Card>
          </Animated.View>
        )}
        {recognition.state.status === "success" && (
          <RecognitionResult result={recognition.state.result} />
        )}
      </View>
    </ScrollView>
  );
}
