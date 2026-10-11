import { ScrollView } from "react-native";
import { Heading } from "@/components/ui";

export default function HistoryScreen() {
  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      contentContainerStyle={{ padding: 24 }}
    >
      <Heading
        title="Your discoveries"
        description="Your recognition history, all in one place."
      />
    </ScrollView>
  );
}
