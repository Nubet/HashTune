package com.norbertfila.hashtune.configuration;

import com.norbertfila.hashtune.security.AuthenticatedIdentityResolver;
import com.norbertfila.hashtune.security.RateLimitInterceptor;
import com.norbertfila.hashtune.security.RequestRateLimiter;
import io.minio.MinioClient;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
    StorageProperties.class,
    AudioProperties.class,
    AudioSafetyProperties.class,
    IndexingProperties.class,
    MtgJamendoSeedProperties.class,
    RateLimitProperties.class,
    IdentityProviderProperties.class
})
public class ApplicationConfiguration implements WebMvcConfigurer {
    private final RequestRateLimiter requestRateLimiter;
    private final AuthenticatedIdentityResolver identityResolver;

    public ApplicationConfiguration(
            RequestRateLimiter requestRateLimiter, AuthenticatedIdentityResolver identityResolver) {
        this.requestRateLimiter = requestRateLimiter;
        this.identityResolver = identityResolver;
    }

    @Bean
    MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    RestClient deezerClient() {
        return RestClient.builder().baseUrl("https://api.deezer.com").build();
    }

    @Bean
    Executor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("indexing-");
        executor.initialize();
        return executor;
    }

    @Bean
    Executor artistImageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("artist-images-");
        executor.initialize();
        return executor;
    }

    @Bean
    CorsFilter corsFilter(@Value("${app.web.allowed-origin:http://localhost:3000}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigin.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(requestRateLimiter, identityResolver))
                .addPathPatterns("/api/**");
    }
}
