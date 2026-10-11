import { ScrollView } from "react-native";
import { Heading } from "@/components/ui";

export default function LibraryScreen() {
  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={{ padding: 24 }}
    >
      <Heading
        title="Explore the library"
        description="Browse the music that HashTune can identify."
      />
    </ScrollView>
  );
}
