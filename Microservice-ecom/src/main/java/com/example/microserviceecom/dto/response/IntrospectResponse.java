package com.example.microserviceecom.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectResponse {
    private String userId  ;
    private boolean isValid;

}
