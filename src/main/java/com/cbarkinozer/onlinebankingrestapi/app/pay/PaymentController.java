package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.app.gen.dto.RestResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/outbound-payments")
public class PaymentController {
    private final PaymentRenderer renderer;
    private final PaymentPublisher publisher;

    @PostMapping("/payloads")
    public ResponseEntity<RestResponse<PaymentPayloads>> payloads(@Valid @RequestBody PaymentInstruction instruction) {
        return ResponseEntity.ok(RestResponse.of(renderer.render(instruction)));
    }

    @PostMapping
    public ResponseEntity<RestResponse<PaymentPayloads>> create(@Valid @RequestBody PaymentInstruction instruction) {
        PaymentPayloads payloads = renderer.render(instruction);
        publisher.publish(instruction.getEndToEndId(), payloads.getEvent());
        return ResponseEntity.ok(RestResponse.of(payloads));
    }

    @ExceptionHandler(AddressIncompleteException.class)
    public ResponseEntity<RestResponse<String>> addressIncomplete(AddressIncompleteException e) {
        RestResponse<String> response = RestResponse.error(e.getMessage());
        response.setMessages(e.getMessage());
        return ResponseEntity.unprocessableEntity().body(response);
    }
}
