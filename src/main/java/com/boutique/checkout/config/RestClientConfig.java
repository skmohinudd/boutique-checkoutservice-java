package com.boutique.checkout.config;
import org.springframework.context.annotation.*;import org.springframework.http.client.SimpleClientHttpRequestFactory;import org.springframework.web.client.RestClient;import java.time.Duration;
@Configuration public class RestClientConfig{@Bean RestClient.Builder restClientBuilder(){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(Duration.ofSeconds(2));f.setReadTimeout(Duration.ofSeconds(8));return RestClient.builder().requestFactory(f);}}
