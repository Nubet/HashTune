import { Image } from "expo-image";
import { Music2 } from "lucide-react-native";
import { View } from "react-native";
import { useTheme } from "@/theme/theme";

export function Artwork({
  uri,
  size = 56,
}: {
  uri: string | null | undefined;
  size?: number;
}) {
  const theme = useTheme();
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: Math.min(20, size / 5),
        backgroundColor: theme.subtle,
        overflow: "hidden",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <Music2 size={size * 0.4} color={theme.muted} />
      {uri && (
        <Image
          source={{ uri }}
          contentFit="cover"
          recyclingKey={uri}
          style={{ position: "absolute", width: size, height: size }}
        />
      )}
    </View>
  );
}
