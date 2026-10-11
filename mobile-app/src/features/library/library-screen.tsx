import { useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Text,
  TextInput,
  View,
} from "react-native";
import { Button, Heading, Message } from "@/components/ui";
import { ChoiceChip } from "@/components/choice-chip";
import type { LibraryFilters } from "@/lib/api/music-api";
import { fonts, useTheme } from "@/theme/theme";
import { libraryEntryKey, useLibrary } from "./use-library";
import type { LibraryResource } from "./use-library";
import { LibraryEntryRow } from "./library-entry";

const resources = [
  { value: "tracks", label: "Tracks" },
  { value: "albums", label: "Albums" },
  { value: "artists", label: "Artists" },
] as const;
const origins = [
  { value: "HASH_TUNE", label: "HashTune" },
  { value: "MTG_JAMENDO", label: "Jamendo" },
  { value: "ALL", label: "All" },
] as const;

export default function LibraryScreen() {
  const theme = useTheme();
  const [resource, setResource] = useState<LibraryResource>("tracks");
  const [search, setSearch] = useState("");
  const [filters, setFilters] = useState<LibraryFilters>({
    query: "",
    origin: "HASH_TUNE",
  });
  const library = useLibrary(resource, filters);
  const entries = library.data?.pages.flatMap((page) => page.content) ?? [];

  function browse(artist: string, album?: string) {
    setResource("tracks");
    setSearch("");
    setFilters({
      query: "",
      origin: filters.origin,
      artist,
      ...(album ? { album } : {}),
    });
  }

  return (
    <FlatList
      key={`${resource}:${JSON.stringify(filters)}`}
      contentInsetAdjustmentBehavior="automatic"
      keyboardShouldPersistTaps="handled"
      keyboardDismissMode="on-drag"
      style={{ backgroundColor: theme.canvas }}
      contentContainerStyle={{ padding: 24, paddingBottom: 40 }}
      data={entries}
      keyExtractor={libraryEntryKey}
      refreshing={library.isRefetching && !library.isFetchingNextPage}
      onRefresh={() => {
        void library.refetch();
      }}
      ListHeaderComponent={
        <View style={{ gap: 18, paddingBottom: 18 }}>
          <Heading
            title="Explore the library"
            description="Browse the music that HashTune can identify."
          />
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
            {resources.map((item) => (
              <ChoiceChip
                key={item.value}
                label={item.label}
                selected={resource === item.value}
                onPress={() => {
                  setResource(item.value);
                  setFilters({ query: search.trim(), origin: filters.origin });
                }}
              />
            ))}
          </View>
          <TextInput
            accessibilityLabel="Search the music library"
            placeholder="Search the library"
            placeholderTextColor={theme.muted}
            value={search}
            onChangeText={setSearch}
            onSubmitEditing={() =>
              setFilters({ ...filters, query: search.trim() })
            }
            returnKeyType="search"
            autoCorrect={false}
            style={{
              minHeight: 48,
              borderRadius: 16,
              paddingHorizontal: 16,
              paddingVertical: 12,
              borderWidth: 1,
              borderColor: theme.line,
              backgroundColor: theme.surface,
              color: theme.ink,
              fontFamily: fonts.regular,
              fontSize: 15,
            }}
          />
          <Button
            label="Search"
            onPress={() => setFilters({ ...filters, query: search.trim() })}
          />
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
            {origins.map((item) => (
              <ChoiceChip
                key={item.value}
                label={item.label}
                selected={filters.origin === item.value}
                onPress={() => setFilters({ ...filters, origin: item.value })}
              />
            ))}
          </View>
          {filters.artist && (
            <View style={{ gap: 10 }}>
              <Text
                selectable
                style={{ color: theme.muted, fontFamily: fonts.medium }}
              >
                {filters.album
                  ? `${filters.artist} · ${filters.album}`
                  : filters.artist}
              </Text>
              <Button
                secondary
                label="Back to all music"
                onPress={() => {
                  setSearch("");
                  setFilters({ query: "", origin: filters.origin });
                }}
              />
            </View>
          )}
          {library.data?.pages[0] && (
            <Text
              selectable
              style={{
                fontFamily: fonts.regular,
                fontSize: 12,
                color: theme.muted,
              }}
            >
              {library.data.pages[0].totalElements.toLocaleString("en-US")}{" "}
              {resource}
            </Text>
          )}
          {library.error && (
            <>
              <Message
                error
                title="Library unavailable"
                description={library.error.message}
              />
              <Button
                secondary
                label="Try again"
                onPress={() => {
                  void library.refetch();
                }}
              />
            </>
          )}
        </View>
      }
      ListEmptyComponent={
        library.isPending ? (
          <ActivityIndicator
            color={theme.primaryText}
            accessibilityLabel="Loading library"
          />
        ) : !library.error ? (
          <Message
            title="No music found"
            description="Try another search or choose a different library source."
          />
        ) : null
      }
      ItemSeparatorComponent={() => (
        <View style={{ height: 1, backgroundColor: theme.line }} />
      )}
      renderItem={({ item }) => (
        <LibraryEntryRow
          entry={item}
          onAlbum={(title, artist) => browse(artist, title)}
          onArtist={(name) => browse(name)}
        />
      )}
      ListFooterComponent={
        library.hasNextPage ? (
          <Button
            secondary
            label="Load more"
            busy={library.isFetchingNextPage}
            onPress={() => {
              void library.fetchNextPage();
            }}
          />
        ) : null
      }
    />
  );
}
