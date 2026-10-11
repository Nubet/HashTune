import { Pressable, Text } from "react-native";
import { fonts, useTheme } from "@/theme/theme";

export function ChoiceChip({
  label,
  selected,
  onPress,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
}) {
  const theme = useTheme();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={({ pressed }) => ({
        borderRadius: 24,
        minHeight: 44,
        paddingHorizontal: 16,
        paddingVertical: 12,
        backgroundColor: selected ? theme.ink : theme.subtle,
        opacity: pressed ? 0.8 : 1,
        alignItems: "center",
        justifyContent: "center",
      })}
    >
      <Text
        style={{
          fontFamily: fonts.semibold,
          fontSize: 13,
          color: selected ? theme.canvas : theme.muted,
        }}
      >
        {label}
      </Text>
    </Pressable>
  );
}
