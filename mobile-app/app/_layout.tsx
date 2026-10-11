import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useFonts,
  Geist_400Regular,
  Geist_500Medium,
  Geist_600SemiBold,
  Geist_700Bold,
} from "@expo-google-fonts/geist";
import { Stack } from "expo-router/stack";
import { StatusBar } from "expo-status-bar";
import { ActivityIndicator, View } from "react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { fonts, useTheme } from "@/theme/theme";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: false },
    mutations: { retry: false },
  },
});

export default function RootLayout() {
  const theme = useTheme();
  const [loaded, error] = useFonts({
    Geist_400Regular,
    Geist_500Medium,
    Geist_600SemiBold,
    Geist_700Bold,
  });
  if (!loaded && !error) {
    return (
      <View
        style={{
          flex: 1,
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: theme.canvas,
        }}
      >
        <ActivityIndicator
          accessibilityLabel="Loading HashTune"
          color={theme.primaryText}
        />
      </View>
    );
  }
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <StatusBar style={theme.dark ? "light" : "dark"} />
        <Stack
          screenOptions={{
            contentStyle: { backgroundColor: theme.canvas },
            headerStyle: { backgroundColor: theme.canvas },
            headerTintColor: theme.ink,
            headerTitleStyle: { fontFamily: fonts.semibold },
          }}
        >
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        </Stack>
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
