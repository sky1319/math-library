package com.example.library.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Configuration
public class EbookConfig {

    @Bean(name = "wikisourceHttpClient")
    public OkHttpClient wikisourceHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(40, TimeUnit.SECONDS);

        String proxyUrl = firstNonBlank(System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"));
        if (proxyUrl != null) {
            URI uri = URI.create(proxyUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getPort() < 1) {
                throw new IllegalStateException("HTTPS_PROXY must be an http://host:port URL");
            }
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), uri.getPort())));
        }
        return builder.build();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        if (second != null && !second.isBlank()) return second.trim();
        return null;
    }
}
