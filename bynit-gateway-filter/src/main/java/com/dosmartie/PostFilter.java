package com.dosmartie;

import com.dosmartie.helper.AccessProviders;
import com.dosmartie.helper.Utility;
import com.dosmartie.request.AuthRequest;
import com.dosmartie.response.AuthResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class PostFilter implements GlobalFilter, Ordered {

    @Autowired
    private AccessProviders accessProviders;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    static AtomicReference<String> email = new AtomicReference<>("");
    static AtomicReference<String> roles = new AtomicReference<>("");


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        if (url.contains("sign-in")) {

            return new ModifyRequestBodyGatewayFilterFactory().apply(readRequestBody()).filter(writeResponse(exchange), chain);
        } else {
            return chain.filter(exchange);
        }
    }


    @Override
    public int getOrder() {
        return -2;
    }

    private ModifyRequestBodyGatewayFilterFactory.Config readRequestBody() {
        return new ModifyRequestBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (request, originalRequestBody) -> {
                    AuthRequest authRequest = null;
                    try {
                        authRequest = mapper.readValue(originalRequestBody, AuthRequest.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    email.lazySet(authRequest.getEmail());
                    roles.lazySet(authRequest.getRole().name());
                    return Mono.just(originalRequestBody);
                });
    }

    private ServerHttpResponseDecorator getDecoratedResponse(ServerWebExchange exchange) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(fluxBody.map((DataBuffer dataBuffer) -> {
                        try {
                            var content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            byte[] updatedContent = getUpdatedContent(content, exchange);
                            return exchange.getResponse().bufferFactory().wrap(updatedContent);
                        } catch (Exception exception) {
                            return null;
                        }
                    }));
                }
                return super.writeWith(body);
            }
        };
    }

    private byte[] getUpdatedContent(byte[] content, ServerWebExchange exchange) throws JsonProcessingException {
        HttpStatusCode responseStatus = exchange.getResponse().getStatusCode();
        assert responseStatus != null;
        if (responseStatus.equals(HttpStatus.OK)) {
            String uuid = generateUUID();
            redisTemplate.opsForValue().set(uuid, jwtTokenUtil.generateToken(new AuthRequest(email.get(), null, Utility.roleConvertor(roles.get()))));
            redisTemplate.expire(uuid, 1, TimeUnit.HOURS);
            return mapper.writeValueAsBytes(new AuthResponse(uuid));
        } else {
            throw new RuntimeException("Unable to write bytes");
        }
    }

    private ServerWebExchange writeResponse(ServerWebExchange
                                             exchange) {
        ServerHttpResponseDecorator decoratedResponse = getDecoratedResponse(exchange);
        return exchange.mutate().response(decoratedResponse).build();
    }

    private String generateUUID() {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final int[] LENGTHS = {8, 4, 4, 4, 12};
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        sb.append("AT-");
        for (int length : LENGTHS) {
            for (int i = 0; i < length; i++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            sb.append("-");
        }
        sb.setLength(sb.length() - 1);

        return sb.toString();
    }
}
