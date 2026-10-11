import { useState } from "react";
import { Text, View } from "react-native";
import Animated, { FadeIn, ReduceMotion } from "react-native-reanimated";
import type { Recognition } from "@/lib/api/contracts";
import { Button, Card, Message } from "@/components/ui";
import { Artwork } from "@/components/artwork";
import { fonts, useTheme } from "@/theme/theme";

export function RecognitionResult({ result }: { result: Recognition }) {
  const theme = useTheme();
  const [details, setDetails] = useState(false);
  const matched = result.status === "MATCHED" && result.track;
  return (
    <Animated.View
      entering={FadeIn.duration(200).reduceMotion(ReduceMotion.System)}
      accessibilityLiveRegion="polite"
    >
      <Card>
        {matched ? (
          <>
            <Text
              selectable
              style={{
                fontFamily: fonts.semibold,
                fontSize: 12,
                letterSpacing: 1.5,
                color: theme.primaryText,
              }}
            >
              SONG IDENTIFIED
            </Text>
            <View
              style={{ flexDirection: "row", gap: 16, alignItems: "center" }}
            >
              <Artwork uri={result.track?.coverArtUrl} size={80} />
              <View style={{ flex: 1, gap: 6 }}>
                <Text
                  selectable
                  style={{
                    fontFamily: fonts.bold,
                    fontSize: 24,
                    letterSpacing: -0.6,
                    color: theme.ink,
                  }}
                >
                  {result.track?.title}
                </Text>
                <Text
                  selectable
                  style={{
                    fontFamily: fonts.medium,
                    fontSize: 16,
                    color: theme.muted,
                  }}
                >
                  {result.track?.artist}
                </Text>
                {result.track?.album && (
                  <Text
                    selectable
                    style={{
                      fontFamily: fonts.regular,
                      fontSize: 13,
                      color: theme.muted,
                    }}
                  >
                    {result.track.album}
                  </Text>
                )}
              </View>
            </View>
          </>
        ) : (
          <Message
            title={
              result.status === "INVALID_AUDIO"
                ? "Audio could not be read"
                : "Nothing matched"
            }
            description={
              result.status === "INVALID_AUDIO"
                ? "Try recording another short clip."
                : "Try a clearer clip or a different part of the song. HashTune recognizes music available in its library."
            }
          />
        )}
        <Button
          secondary
          label={details ? "Hide details" : "View details"}
          onPress={() => setDetails(!details)}
        />
        {details && (
          <View style={{ gap: 8 }}>
            {result.sampleDurationMs != null && (
              <Text
                selectable
                style={{ fontFamily: fonts.regular, color: theme.muted }}
              >
                Sample length · {(result.sampleDurationMs / 1000).toFixed(1)}{" "}
                seconds
              </Text>
            )}
            {result.recognitionTimeMs != null && (
              <Text
                selectable
                style={{ fontFamily: fonts.regular, color: theme.muted }}
              >
                Search time · {(result.recognitionTimeMs / 1000).toFixed(2)}{" "}
                seconds
              </Text>
            )}
          </View>
        )}
      </Card>
    </Animated.View>
  );
}
