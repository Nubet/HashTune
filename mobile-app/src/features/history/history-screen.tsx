import { useState } from "react";
import { ActivityIndicator, Alert, FlatList, Text, View } from "react-native";
import { Button, Card, Heading, Message } from "@/components/ui";
import { TrackRow } from "@/components/track-row";
import { SessionStatus } from "@/components/session-status";
import { fonts, useTheme } from "@/theme/theme";
import { useHistory } from "./use-history";
import { RecordingPlayer } from "./recording-player";

export default function HistoryScreen() {
  const theme = useTheme();
  const { session, history, clear } = useHistory();
  const [playingId, setPlayingId] = useState<string | null>(null);
  const items = history.data?.pages.flat() ?? [];

  function confirmClear() {
    Alert.alert(
      "Clear your history?",
      "This removes all recognition history and saved recordings for this session.",
      [
        { text: "Keep history", style: "cancel" },
        {
          text: "Clear history",
          style: "destructive",
          onPress: () => clear.mutate(),
        },
      ],
    );
  }

  return (
    <FlatList
      contentInsetAdjustmentBehavior="automatic"
      style={{ backgroundColor: theme.canvas }}
      contentContainerStyle={{ padding: 24, gap: 16, paddingBottom: 40 }}
      data={items}
      keyExtractor={(item) => item.id}
      refreshing={history.isRefetching && !history.isFetchingNextPage}
      onRefresh={() => {
        if (session.data && !clear.isPending) void history.refetch();
      }}
      ListHeaderComponent={
        <View style={{ gap: 20 }}>
          <Heading
            title="Your discoveries"
            description="Every search, saved to your private session."
          />
          <SessionStatus />
          {items.length > 0 && (
            <Button
              secondary
              label="Clear history"
              disabled={clear.isPending || history.isFetching}
              busy={clear.isPending}
              onPress={confirmClear}
            />
          )}
          {clear.error && (
            <Message
              error
              title="History could not be cleared"
              description={clear.error.message}
            />
          )}
          {history.error && (
            <>
              <Message
                error
                title="History unavailable"
                description={history.error.message}
              />
              <Button
                label="Try again"
                onPress={() => {
                  void history.refetch();
                }}
              />
            </>
          )}
        </View>
      }
      ListEmptyComponent={
        session.data && !history.error ? (
          history.isPending ? (
            <ActivityIndicator
              color={theme.primaryText}
              accessibilityLabel="Loading history"
            />
          ) : (
            <Message
              title="No history yet"
              description="Identify your first song. Your searches will appear here."
            />
          )
        ) : null
      }
      renderItem={({ item }) => (
        <Card>
          {item.track ? (
            <TrackRow track={item.track} />
          ) : (
            <Message
              title={
                item.status === "INVALID_AUDIO"
                  ? "Audio could not be read"
                  : "Nothing matched"
              }
              description="No track was identified in this search."
            />
          )}
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 10 }}>
            <Text
              selectable
              style={{
                fontFamily: fonts.medium,
                fontSize: 12,
                color: theme.primaryText,
              }}
            >
              {item.status === "MATCHED" ? "Match found" : "No match"}
            </Text>
            <Text
              selectable
              style={{
                fontFamily: fonts.regular,
                fontSize: 12,
                color: theme.muted,
              }}
            >
              {item.source === "MICROPHONE" ? "Microphone" : "Audio file"}
            </Text>
            <Text
              selectable
              style={{
                fontFamily: fonts.regular,
                fontSize: 12,
                color: theme.muted,
              }}
            >
              {new Date(item.createdAt).toLocaleString("en-US", {
                month: "short",
                day: "numeric",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </Text>
          </View>
          {item.source === "MICROPHONE" &&
            item.recordingUrl &&
            (playingId === item.id ? (
              <RecordingPlayer key={item.id} recognitionId={item.id} />
            ) : (
              <Button
                secondary
                label="Listen to sample"
                disabled={clear.isPending}
                onPress={() => setPlayingId(item.id)}
              />
            ))}
        </Card>
      )}
      ListFooterComponent={
        history.hasNextPage ? (
          <Button
            secondary
            busy={history.isFetchingNextPage}
            disabled={clear.isPending}
            label="Load more"
            onPress={() => {
              void history.fetchNextPage();
            }}
          />
        ) : null
      }
    />
  );
}
