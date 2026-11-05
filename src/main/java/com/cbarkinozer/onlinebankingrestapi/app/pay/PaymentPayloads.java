package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.app.gen.util.AccountDisclosure;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Value;

@Value
public class PaymentPayloads {
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    String mt103;
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    String pacs008;
    @JsonSerialize(using = AccountDisclosure.TextSerializer.class)
    String event;
}
