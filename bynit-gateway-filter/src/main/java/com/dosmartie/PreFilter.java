package com.dosmartie;

import com.dosmartie.common.Roles;
import com.dosmartie.helper.AccessProviders;
import com.dosmartie.helper.Utility;
import com.dosmartie.request.AuthRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class PreFilter implements GlobalFilter, Ordered {
    private final AccessProviders accessProviders;

    private final RedisTemplate<String, String> redisTemplate;

    private final JwtTokenUtil jwtTokenUtil;
    @Autowired
    private ObjectMapper mapper;


    @Autowired
    public PreFilter(AccessProviders accessProviders, RedisTemplate<String, String> redisTemplate, JwtTokenUtil jwtTokenUtil) {
        this.accessProviders = accessProviders;
        this.redisTemplate = redisTemplate;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("This is the pre filter...");
        try {
            String url = exchange.getRequest().getURI().getPath();
//            Flux<String> data = exchange.getRequest().getBody()
//                            .map(dataBuffer -> {
//                                try {
//                                    System.out.println(new String(dataBuffer.asInputStream().readAllBytes(), StandardCharsets.UTF_8));
//                                    return new String(dataBuffer.asInputStream().readAllBytes());
//                                } catch (IOException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            });
//            System.out.println("Data" + data.);
            if (canAllowWithoutToken(url)) {
                    return chain.filter(exchange);
            } else {
                if (Objects.isNull(exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION))) {
                    return Mono.error(NoTokenFoundException::new);
                } else {
                    try {
                        List<String> token = Objects.requireNonNull(exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION)).stream().filter(header -> header.contains("Bearer ")).toList();
                        String jwtToken = token.get(0).substring(7);
                        String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                        return blockUserBasedOnRole(Utility.roleConvertor(jwtTokenUtil.getRoleFromToken(jwtToken)), url, exchange, username, chain);
                    } catch (SessionTimeOutException | ExpiredJwtException exception) {
                        log.error(exception.getLocalizedMessage());
                        return Mono.error(exception);
                    } catch (Exception exception) {
                        log.error(exception.getLocalizedMessage());
                        return chain.filter(exchange);
                    }
                }
            }
        }
        catch (Exception exception) {
            log.error(exception.getLocalizedMessage());
            return Mono.error(exception);
        }
    }



    private Mono<Void> blockUserBasedOnRole(Roles role, String url, ServerWebExchange exchange, String username, GatewayFilterChain chain) {
        switch (role) {
            case ADMIN -> {
               return canAllowAdmin(url, chain, exchange);
            }
            case MERCHANT -> {
                return canAllowMerchant(url, exchange, username, chain);
            }
            case SUPER_ADMIN -> {
                return canAllowSuperAdmin(url, chain, exchange);
            }
            default -> {
                return canAllowUser(url, chain, exchange, username);
            }
        }
    }

    private boolean canAllowWithoutToken(String url) {
       return Arrays.stream(accessProviders.getPermittedUrlWithoutToken()).anyMatch(url::contains);
    }

    private Mono<Void> canAllowUser(String url, GatewayFilterChain chain,ServerWebExchange exchange, String username) {
        if (Arrays.stream(accessProviders.getUserAuthorizedUrl()).anyMatch(url::contains)) {
            if (url.contains("cart")) {
                ServerHttpRequest request = exchange.getRequest()
                        .mutate()
                        .header("email", username)
                        .build();
                ServerWebExchange exchangeHeaders = exchange.mutate().request(request).build();
                return chain.filter(exchangeHeaders);
            }
            return chain.filter(exchange);
        }
        return Mono.error(InvalidAccessException::new);
    }
    private Mono<Void> canAllowMerchant(String url, ServerWebExchange exchange, String username, GatewayFilterChain chain) {
        if (Arrays.stream(accessProviders.getMerchantAuthorizedUrl()).anyMatch(url::contains) && url.matches(accessProviders.getMerchantExceptionRegex())) {
            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("merchant",username)
                    .build();
            ServerWebExchange exchangeHeaders = exchange.mutate().request(request).build();
            return chain.filter(exchangeHeaders);
        }
        return Mono.error(InvalidAccessException::new);
    }
    private Mono<Void> canAllowAdmin(String url, GatewayFilterChain chain, ServerWebExchange exchange) {
        if(Arrays.stream(accessProviders.getAdminAuthorizedUrl()).anyMatch(url::contains)) {
            return chain.filter(exchange);
        }
        return Mono.error(InvalidAccessException::new);
    }
    private Mono<Void> canAllowSuperAdmin(String url, GatewayFilterChain chain, ServerWebExchange exchange) {
        if(Arrays.stream(accessProviders.getSuperAdminAuthorizedUrl()).anyMatch(url::contains)) {
            return chain.filter(exchange);
        }
        return Mono.error(InvalidAccessException::new);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
