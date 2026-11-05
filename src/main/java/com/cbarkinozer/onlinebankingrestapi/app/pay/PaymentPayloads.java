package com.cbarkinozer.onlinebankingrestapi.app.pay;

import lombok.Value;

@Value
public class PaymentPayloads {
    String mt103;
    String pacs008;
    String event;
}
