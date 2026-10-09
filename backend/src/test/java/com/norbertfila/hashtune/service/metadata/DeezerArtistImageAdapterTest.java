package com.norbertfila.hashtune.service.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeezerArtistImageAdapterTest {
    @Test
    void returnsImageOnlyForAnExactArtistMatch() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.deezer.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deezer.com/search/artist?q=Josef%20Bratan&limit=10"))
                .andRespond(withSuccess(
                        """
                        {"data":[
                          {"name":"Josef","picture_xl":"https://example.com/wrong.jpg"},
                          {"name":"Josef Bratan","picture_xl":"https://example.com/josef.jpg"}
                        ]}
                        """,
                        MediaType.APPLICATION_JSON));

        DeezerArtistImageAdapter adapter = new DeezerArtistImageAdapter(builder.build());

        assertThat(adapter.findArtistImage("Josef Bratan")).contains("https://example.com/josef.jpg");
        server.verify();
    }
}
