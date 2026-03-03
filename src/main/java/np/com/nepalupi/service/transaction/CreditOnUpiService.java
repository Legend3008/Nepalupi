package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.CreditOnUpiCard;
import np.com.nepalupi.repository.CreditOnUpiCardRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Credit on UPI service.
 * Allows users to link credit cards and pay via UPI using credit line.
 * Similar to RuPay Credit on UPI feature.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditOnUpiService {

    private final CreditOnUpiCardRepository cardRepository;

    /**
     * Link a credit card to a UPI VPA for credit-on-UPI payments.
     */
    public CreditOnUpiCard linkCard(UUID userId, String cardIssuer, String cardLastFour,
                                     String cardNetwork, String linkedVpa,
                                     Long creditLimitPaisa) {
        CreditOnUpiCard card = CreditOnUpiCard.builder()
                .userId(userId)
                .cardIssuer(cardIssuer)
                .cardLastFour(cardLastFour)
                .cardNetwork(cardNetwork != null ? cardNetwork : "RUPAY")
                .linkedVpa(linkedVpa)
                .creditLimitPaisa(creditLimitPaisa)
                .availableLimitPaisa(creditLimitPaisa) // Initially full limit available
                .isActive(true)
                .linkedAt(Instant.now())
                .build();

        cardRepository.save(card);
        log.info("Credit card linked: userId={}, issuer={}, last4={}, vpa={}",
                userId, cardIssuer, cardLastFour, linkedVpa);
        return card;
    }

    /**
     * Delink a credit card.
     */
    public void delinkCard(UUID cardId) {
        CreditOnUpiCard card = findById(cardId);
        card.setIsActive(false);
        cardRepository.save(card);
        log.info("Credit card delinked: cardId={}", cardId);
    }

    /**
     * Check available credit limit before transaction.
     */
    public boolean hasAvailableLimit(UUID cardId, Long amountPaisa) {
        CreditOnUpiCard card = findById(cardId);
        return Boolean.TRUE.equals(card.getIsActive())
                && card.getAvailableLimitPaisa() != null
                && card.getAvailableLimitPaisa() >= amountPaisa;
    }

    /**
     * Debit the credit limit after a successful credit-on-UPI transaction.
     */
    public CreditOnUpiCard debitLimit(UUID cardId, Long amountPaisa) {
        CreditOnUpiCard card = findById(cardId);
        if (!hasAvailableLimit(cardId, amountPaisa)) {
            throw new IllegalStateException("Insufficient credit limit");
        }
        card.setAvailableLimitPaisa(card.getAvailableLimitPaisa() - amountPaisa);
        cardRepository.save(card);
        log.info("Credit limit debited: cardId={}, amount={}, remaining={}", cardId, amountPaisa, card.getAvailableLimitPaisa());
        return card;
    }

    /**
     * Restore credit limit (for reversed/refunded transactions).
     */
    public CreditOnUpiCard restoreLimit(UUID cardId, Long amountPaisa) {
        CreditOnUpiCard card = findById(cardId);
        long newLimit = Math.min(
                card.getAvailableLimitPaisa() + amountPaisa,
                card.getCreditLimitPaisa()
        );
        card.setAvailableLimitPaisa(newLimit);
        cardRepository.save(card);
        log.info("Credit limit restored: cardId={}, amount={}, newLimit={}", cardId, amountPaisa, newLimit);
        return card;
    }

    /**
     * Get all active linked cards for a user.
     */
    public List<CreditOnUpiCard> getActiveCards(UUID userId) {
        return cardRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get all cards (including delinked) for a user.
     */
    public List<CreditOnUpiCard> getAllCards(UUID userId) {
        return cardRepository.findByUserId(userId);
    }

    private CreditOnUpiCard findById(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found: " + id));
    }
}
