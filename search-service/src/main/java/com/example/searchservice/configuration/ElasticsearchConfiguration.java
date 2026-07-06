package com.example.searchservice.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class ElasticsearchConfiguration {

    @Value("${elasticsearch.server-url}")
    private String serverUrl;

    @Value("${elasticsearch.api-key}")
    private String apiKey;

    @Bean
    ElasticsearchClient elasticsearchClient() throws URISyntaxException {
        // Tạo Rest5Client với API Key authentication
        Rest5Client rest5Client = Rest5Client.builder(HttpHost.create(serverUrl))
                .setDefaultHeaders(new Header[] {
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //Tạo Transport layer với Jackson JSON mapper
        Rest5ClientTransport transport = new Rest5ClientTransport(
                rest5Client,
                new JacksonJsonpMapper(objectMapper)
        );
        //Tạo ElasticsearchClient
        return new ElasticsearchClient(transport);
    }
}
