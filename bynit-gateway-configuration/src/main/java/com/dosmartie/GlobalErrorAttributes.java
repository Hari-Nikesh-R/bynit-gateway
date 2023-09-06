package com.dosmartie;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorResponse = super.getErrorAttributes(request, options);
        HttpStatus status = HttpStatus.valueOf((Integer) errorResponse.get("status"));
        errorResponse.remove("path");
        errorResponse.remove("requestId");
        errorResponse.remove("timestamp");
        if (status == HttpStatus.UNAUTHORIZED) {
            errorResponse.put("message", "Token expired");
        } else if (status == HttpStatus.FORBIDDEN){
            errorResponse.put("message", "Invalid access");
        } else {
            errorResponse.put("message", "Something went wrong");
        }
        return errorResponse;
    }
}
