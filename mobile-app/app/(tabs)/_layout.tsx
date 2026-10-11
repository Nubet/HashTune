import { Tabs } from "expo-router";
import { AudioLines, Library, History } from "lucide-react-native";
import { Image } from "expo-image";
import { fonts, useTheme } from "@/theme/theme";

export default function TabLayout() {
  const theme = useTheme();
  return (
    <Tabs
      screenOptions={{
        headerTitle: "HashTune",
        headerTitleStyle: { fontFamily: fonts.semibold },
        headerStyle: { backgroundColor: theme.canvas },
        headerTintColor: theme.ink,
        headerShadowVisible: false,
        headerLeft: () => (
          <Image
            source={
              theme.dark
                ? require("../../assets/logo-white.png")
                : require("../../assets/logo-blue.png")
            }
            style={{ width: 28, height: 28, marginLeft: 20 }}
          />
        ),
        tabBarStyle: {
          backgroundColor: theme.surface,
          borderTopColor: theme.line,
        },
        tabBarActiveTintColor: theme.primaryText,
        tabBarInactiveTintColor: theme.muted,
        tabBarLabelStyle: { fontFamily: fonts.medium, fontSize: 11 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Identify",
          tabBarIcon: ({ color, size }) => (
            <AudioLines color={color} size={size} />
          ),
        }}
      />
      <Tabs.Screen
        name="library"
        options={{
          title: "Library",
          tabBarIcon: ({ color, size }) => (
            <Library color={color} size={size} />
          ),
        }}
      />
      <Tabs.Screen
        name="history"
        options={{
          title: "History",
          tabBarIcon: ({ color, size }) => (
            <History color={color} size={size} />
          ),
        }}
      />
    </Tabs>
  );
}
