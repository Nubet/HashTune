import { ScrollView } from "react-native";
import { Heading } from "@/components/ui";

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
    </ScrollView>
  );
}
