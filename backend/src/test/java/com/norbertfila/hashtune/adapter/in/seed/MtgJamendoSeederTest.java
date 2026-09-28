package com.norbertfila.hashtune.adapter.in.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MtgJamendoSeederTest {
    @Test
    void parsesQuotedCommasAndEscapedQuotes() {
        List<Map<String, String>> rows = MtgJamendoSeeder.parseCsv(
                "track_id,title,artist,album\n1,Song,Artist,\"Album, Vol. 1\"\n2,\"Say \"\"hello\"\"\",Artist,Album\n");

        assertThat(rows).extracting(row -> row.get("album")).containsExactly("Album, Vol. 1", "Album");
        assertThat(rows.get(1).get("title")).isEqualTo("Say \"hello\"");
    }
}
