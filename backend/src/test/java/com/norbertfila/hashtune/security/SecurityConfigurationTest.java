package com.norbertfila.hashtune.security;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.norbertfila.hashtune.configuration.ApplicationConfiguration;
import com.norbertfila.hashtune.configuration.IdentityProviderProperties;
import com.norbertfila.hashtune.controller.indexing.IndexingJobController;
import com.norbertfila.hashtune.controller.library.LibraryAdminController;
import com.norbertfila.hashtune.controller.library.LibraryQueryController;
import com.norbertfila.hashtune.controller.recognition.RecognitionController;
import com.norbertfila.hashtune.entity.indexing.IndexingJob;
import com.norbertfila.hashtune.entity.indexing.IndexingJobStatus;
import com.norbertfila.hashtune.repository.track.PageResult;
import com.norbertfila.hashtune.service.image.ArtistImageService;
import com.norbertfila.hashtune.service.indexing.IndexingJobService;
import com.norbertfila.hashtune.service.recognition.RecognitionService;
import com.norbertfila.hashtune.service.track.TrackQueryService;
import com.norbertfila.hashtune.service.track.TrackService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {
            LibraryQueryController.class,
            LibraryAdminController.class,
            IndexingJobController.class,
            RecognitionController.class
        },
        properties = {
            "app.identity-provider.enabled=true",
            "app.identity-provider.issuer-uri=https://issuer.example.com",
            "app.identity-provider.jwk-set-uri=https://issuer.example.com/.well-known/jwks.json",
            "app.identity-provider.audience=authenticated"
        },
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationConfiguration.class))
@Import({SecurityConfiguration.class, SecurityConfigurationTest.TestSecurityConfiguration.class})
class SecurityConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackQueryService trackQueryService;

    @MockitoBean
    private ArtistImageService artistImageService;

    @MockitoBean
    private TrackService trackApplicationService;

    @MockitoBean
    private IndexingJobService indexingJobApplicationService;

    @MockitoBean
    private RecognitionService recognitionApplicationService;

    @MockitoBean
    private AuthenticatedIdentityResolver authenticatedIdentityResolver;

    @BeforeEach
    void configureJwtMode() {
        given(trackQueryService.searchTracks(org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageResult<>(List.of(), 0, 25, 0));
    }

    @Test
    void allowsPublicLibraryReadWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/library/tracks")).andExpect(status().isOk());
    }

    @Test
    void rejectsUnlistedLibraryGetWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/library/internal")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRecognitionHistoryWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/recognition-history")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAdminEndpointForRegularUser() throws Exception {
        mockMvc.perform(get("/api/v1/indexing-jobs/{id}", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminEndpointForAdministrator() throws Exception {
        UUID jobId = UUID.randomUUID();
        given(indexingJobApplicationService.get(jobId))
                .willReturn(new IndexingJob(
                        jobId,
                        UUID.randomUUID(),
                        IndexingJobStatus.PENDING,
                        0,
                        0,
                        null,
                        null,
                        Instant.now(),
                        null,
                        null));

        mockMvc.perform(get("/api/v1/indexing-jobs/{id}", jobId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsMalformedMetadataRequestAsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/library/tracks/{id}/metadata", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsInvalidPageSizeAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/library/tracks").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityConfiguration {
        @Bean
        IdentityProviderProperties identityProviderProperties() {
            IdentityProviderProperties properties = new IdentityProviderProperties();
            properties.setEnabled(true);
            properties.setIssuerUri("https://issuer.example.com");
            properties.setJwkSetUri("https://issuer.example.com/.well-known/jwks.json");
            properties.setAudience("authenticated");
            return properties;
        }
    }
}
