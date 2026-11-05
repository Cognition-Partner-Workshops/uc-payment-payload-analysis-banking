package com.cbarkinozer.onlinebankingrestapi.app.pay;

import com.cbarkinozer.onlinebankingrestapi.app.acc.entity.AccAccount;
import com.cbarkinozer.onlinebankingrestapi.app.acc.enums.AccErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.acc.service.entityservice.AccAccountEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.dto.RestResponse;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
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
    private final AccAccountEntityService accAccountEntityService;

    @PostMapping("/payloads")
    public ResponseEntity<RestResponse<PaymentPayloads>> payloads(@Valid @RequestBody PaymentInstruction instruction) {
        return ResponseEntity.ok(RestResponse.of(renderer.render(instruction)));
    }

    @PostMapping
    public ResponseEntity<RestResponse<PaymentPayloads>> create(@Valid @RequestBody PaymentInstruction instruction) {
        PaymentPayloads payloads = renderer.render(instruction);
        controlIsDebtorAccountOwnedByCurrentCustomer(instruction);
        publisher.publish(instruction.getEndToEndId(), payloads.getEvent());
        return ResponseEntity.ok(RestResponse.of(payloads));
    }

    private void controlIsDebtorAccountOwnedByCurrentCustomer(PaymentInstruction instruction) {
        AccAccount debtorAccount = accAccountEntityService
                .getOwnedActiveAccountByIbanOrThrow(instruction.getDebtor().getIban());
        if (debtorAccount.getCurrencyType() == null
                || !debtorAccount.getCurrencyType().getIsoCode().equals(instruction.getCurrency())) {
            throw new IllegalFieldException(AccErrorMessage.CURRENCY_MISMATCH);
        }
    }
}
