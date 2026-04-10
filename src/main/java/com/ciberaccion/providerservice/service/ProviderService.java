package com.ciberaccion.providerservice.service;

import com.ciberaccion.providerservice.dto.CardValidationRequest;
import com.ciberaccion.providerservice.dto.CardValidationResponse;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ProviderService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000");
    private static final String BLOCKED_CARD = "4111111111111111";

    public CardValidationResponse validate(CardValidationRequest request) {

        // tarjeta bloqueada
        if (BLOCKED_CARD.equals(request.getCardNumber())) {
            return new CardValidationResponse(false, "Card is blocked");
        }

        // monto excede limite
        if (request.getAmount().compareTo(MAX_AMOUNT) > 0) {
            return new CardValidationResponse(false, "Amount exceeds transaction limit");
        }

        // solo Visa (4) y Mastercard (5)
        String firstDigit = request.getCardNumber().substring(0, 1);
        if (!firstDigit.equals("4") && !firstDigit.equals("5")) {
            return new CardValidationResponse(false, "Card network not supported");
        }

        return new CardValidationResponse(true, "Approved by provider");
    }
}