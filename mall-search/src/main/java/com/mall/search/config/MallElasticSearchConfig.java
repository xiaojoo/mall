package com.mall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class MallElasticSearchConfig {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String esUri;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        HttpHost[] hosts = Arrays.stream(esUri.split(","))
                .map(String::trim)
                .map(uri -> {
                    try {
                        java.net.URI u = new java.net.URI(uri.startsWith("http") ? uri : "http://" + uri);
                        return new HttpHost(u.getHost(), u.getPort() == -1 ? 9200 : u.getPort(), u.getScheme());
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid ES URI: " + uri, e);
                    }
                })
                .toArray(HttpHost[]::new);
        return new RestHighLevelClient(RestClient.builder(hosts));
    }
}
