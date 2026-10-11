import type { PropsWithChildren } from "react";
import { ActivityIndicator, Pressable, Text, View } from "react-native";
import { fonts, useTheme } from "@/theme/theme";

export function Button({
  label,
  onPress,
  disabled = false,
  secondary = false,
  busy = false,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  secondary?: boolean;
  busy?: boolean;
}) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: disabled || busy, busy }}
      disabled={disabled || busy}
      onPress={onPress}
      style={({ pressed }) => ({
        minHeight: 48,
        paddingHorizontal: 20,
        paddingVertical: 12,
        borderRadius: 24,
        backgroundColor: secondary ? theme.subtle : theme.primary,
        opacity: disabled || busy ? 0.55 : pressed ? 0.8 : 1,
        alignItems: "center",
        justifyContent: "center",
        flexDirection: "row",
        gap: 10,
      })}
    >
      {busy && (
        <ActivityIndicator color={secondary ? theme.ink : theme.white} />
      )}
      <Text
        style={{
          fontFamily: fonts.semibold,
          fontSize: 15,
          color: secondary ? theme.ink : theme.white,
        }}
      >
        {label}
      </Text>
    </Pressable>
  );
}

export function Card({ children }: PropsWithChildren) {
  const theme = useTheme();
  return (
    <View
      style={{
        padding: 20,
        gap: 14,
        borderRadius: 24,
        borderCurve: "continuous",
        borderWidth: 1,
        borderColor: theme.line,
        backgroundColor: theme.surface,
      }}
    >
      {children}
    </View>
  );
}

export function Heading({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  const theme = useTheme();
  return (
    <View style={{ gap: 8 }}>
      <Text
        selectable
        style={{
          fontSize: 30,
          fontFamily: fonts.bold,
          color: theme.ink,
          letterSpacing: -1,
        }}
      >
        {title}
      </Text>
      <Text
        selectable
        style={{
          fontSize: 15,
          lineHeight: 23,
          fontFamily: fonts.regular,
          color: theme.muted,
        }}
      >
        {description}
      </Text>
    </View>
  );
}

export function Message({
  title,
  description,
  error = false,
}: {
  title: string;
  description: string;
  error?: boolean;
}) {
  const theme = useTheme();
  return (
    <View
      accessibilityLiveRegion={error ? "polite" : "none"}
      style={{ gap: 8, paddingVertical: 20 }}
    >
      <Text
        selectable
        style={{
          fontFamily: fonts.semibold,
          fontSize: 18,
          color: error ? theme.danger : theme.ink,
        }}
      >
        {title}
      </Text>
      <Text
        selectable
        style={{
          fontFamily: fonts.regular,
          fontSize: 14,
          lineHeight: 22,
          color: theme.muted,
        }}
      >
        {description}
      </Text>
    </View>
  );
}
