import { useCallback, useMemo, useState } from "react";
import { ActivityIndicator, Alert, FlatList, View } from "react-native";
import type { ListRenderItemInfo } from "react-native";
import type { HistoryItem } from "@/lib/api/contracts";
import { Button, Heading, Message } from "@/components/ui";
import { SessionStatus } from "@/components/session-status";
import { useTheme } from "@/theme/theme";
import { useHistory } from "./use-history";
import { HistoryEntry } from "./history-entry";

export default function HistoryScreen() {
  const theme = useTheme();
  const { session, history, clear } = useHistory();
  const [playingId, setPlayingId] = useState<string | null>(null);
  const items = useMemo(() => {
    const seen = new Set<string>();
    return (history.data?.pages.flat() ?? []).filter((item) => {
      if (seen.has(item.id)) return false;
      seen.add(item.id);
      return true;
    });
  }, [history.data]);
  const renderItem = useCallback(
    ({ item }: ListRenderItemInfo<HistoryItem>) => (
      <HistoryEntry
        item={item}
        active={playingId === item.id}
        disabled={clear.isPending}
        onPlay={setPlayingId}
      />
    ),
    [playingId, clear.isPending],
  );

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
      renderItem={renderItem}
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
