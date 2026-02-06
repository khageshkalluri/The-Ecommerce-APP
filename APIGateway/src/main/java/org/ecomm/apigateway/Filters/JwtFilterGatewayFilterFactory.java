package org.ecomm.apigateway.Filters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;

    public JwtFilterGatewayFilterFactory(@Value("${service.url}") String baseURL, WebClient webClient) {
        this.webClient = WebClient.builder()
                .baseUrl(baseURL)
                .build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange,chain)->{
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if(token==null || !token.startsWith("Bearer ")){
                 exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

           return this.webClient.post()
                    .uri("/auth/validate")
                    .header("Authorization", token)
                    .retrieve()
                    .toBodilessEntity()
                    .then(chain.filter(exchange));
        };
    }

}
