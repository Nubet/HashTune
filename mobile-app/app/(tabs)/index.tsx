import { ScrollView } from "react-native";
import { Heading } from "@/components/ui";
import { SessionStatus } from "@/components/session-status";

export default function ListenScreen() {
  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={{ padding: 24 }}
    >
      <Heading
        title="Discover the music"
        description="Identify the music around you with HashTune."
      />
      <SessionStatus />
    </ScrollView>
  );
}
