package com.unir.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class GlobalFilerImpl implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(GlobalFilerImpl.class);

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Ejecutando pre-filter");

        Optional<String> auth = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("authorization"));
        if (auth.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate()
                .headers(x -> x.setBearerAuth("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")).build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(serverHttpRequest).build();

        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            logger.info("Ejecutando post-filter");

            Optional.ofNullable(mutatedExchange.getRequest().getHeaders().getFirst("authorization"))
                .ifPresent( value -> {
                    mutatedExchange.getResponse().getCookies()
                            .add("color", ResponseCookie.from("color", "green")
                                    .build());
                });

            mutatedExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        }));
    }

}
