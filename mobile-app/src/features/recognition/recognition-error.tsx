import { Linking } from "react-native";
import Animated, { FadeIn, ReduceMotion } from "react-native-reanimated";
import { Button, Card, Message } from "@/components/ui";
import { MicrophonePermissionError } from "./use-recognition";

export function RecognitionError({ error }: { error: Error }) {
  return (
    <Animated.View
      entering={FadeIn.duration(200).reduceMotion(ReduceMotion.System)}
    >
      <Card>
        <Message
          error
          title="Let's try that again"
          description={error.message}
        />
        {error instanceof MicrophonePermissionError && !error.canAskAgain && (
          <Button
            label="Open Settings"
            onPress={() => {
              void Linking.openSettings();
            }}
          />
        )}
      </Card>
    </Animated.View>
  );
}
