package com.cbarkinozer.onlinebankingrestapi.app.kafka.dto;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogMessage implements Serializable {

    private Long id ;
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    @ToString.Exclude
    private String message;
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    @ToString.Exclude
    private String description;
    private LocalDateTime dateTime;
}
