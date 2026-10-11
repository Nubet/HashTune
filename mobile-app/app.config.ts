import type { ExpoConfig } from "expo/config";

const production = process.env.APP_VARIANT === "production";

export default {
  name: "HashTune",
  slug: "hashtune-mobile",
  version: "1.0.0",
  scheme: "hashtune",
  platforms: ["ios", "android"],
  orientation: "portrait",
  userInterfaceStyle: "automatic",
  icon: "./assets/icon.png",
  ios: {
    bundleIdentifier: "com.norbertfila.hashtune",
    supportsTablet: false,
    infoPlist: production
      ? {}
      : {
          NSAppTransportSecurity: { NSAllowsLocalNetworking: true },
          NSLocalNetworkUsageDescription:
            "HashTune connects to your local development backend.",
        },
  },
  android: {
    package: "com.norbertfila.hashtune",
    adaptiveIcon: {
      foregroundImage: "./assets/adaptive-icon.png",
      backgroundColor: "#F7F8FB",
    },
    predictiveBackGestureEnabled: true,
  },
  plugins: [
    "expo-router",
    "expo-dev-client",
    "expo-asset",
    "expo-font",
    "expo-secure-store",
    [
      "expo-audio",
      {
        microphonePermission:
          "HashTune records a short audio sample to identify the music around you.",
        enableBackgroundRecording: false,
        enableBackgroundPlayback: false,
      },
    ],
    [
      "expo-splash-screen",
      {
        image: "./assets/logo-blue.png",
        imageWidth: 120,
        backgroundColor: "#F7F8FB",
        dark: { image: "./assets/logo-white.png", backgroundColor: "#111114" },
      },
    ],
    [
      "expo-build-properties",
      { android: { usesCleartextTraffic: !production } },
    ],
  ],
} satisfies ExpoConfig;
