import { ActivityIndicator, Pressable, Text } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Mic, Square } from "lucide-react-native";
import Animated, {
  ReduceMotion,
  useAnimatedStyle,
  withRepeat,
  withTiming,
} from "react-native-reanimated";
import { fonts, useTheme } from "@/theme/theme";
import type { RecognitionState } from "./recognition-session";

const labels = {
  idle: "Identify",
  preparing: "Getting ready…",
  recording: "Stop & identify",
  processing: "Identifying…",
  canceling: "Canceling…",
  success: "Identify again",
  error: "Try again",
} as const;

export function IdentifyButton({
  status,
  disabled,
  onPress,
}: {
  status: RecognitionState["status"];
  disabled: boolean;
  onPress: () => void;
}) {
  const theme = useTheme();
  const recording = status === "recording";
  const busy =
    status === "preparing" || status === "processing" || status === "canceling";
  const style = useAnimatedStyle(
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
            : withTiming(1, { reduceMotion: ReduceMotion.System }),
        },
      ],
    }),
    [recording],
  );
  return (
    <Animated.View
      style={[
        {
          width: 210,
          height: 210,
          borderRadius: 105,
          backgroundColor: theme.dark ? "#0A58CA20" : "#BFDBFE70",
          padding: 15,
        },
        style,
      ]}
    >
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={
          recording ? "Stop recording and identify music" : "Identify music"
        }
        accessibilityState={{ disabled, busy }}
        disabled={disabled}
        onPress={onPress}
        style={({ pressed }) => ({
          flex: 1,
          borderRadius: 90,
          overflow: "hidden",
          opacity: pressed || disabled ? 0.75 : 1,
        })}
      >
        <LinearGradient
          colors={[theme.primary, "#2563EB"]}
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
              textAlign: "center",
            }}
          >
            {labels[status]}
          </Text>
        </LinearGradient>
      </Pressable>
    </Animated.View>
  );
}
