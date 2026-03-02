package np.com.nepalupi.billpay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.billpay.entity.Bill;
import np.com.nepalupi.billpay.entity.Biller;
import np.com.nepalupi.billpay.service.BillPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bill Payment API — Nepal's BBPS equivalent.
 * <p>
 * Provides endpoints for biller discovery, bill fetch, and bill payment via UPI.
 */
@RestController
@RequestMapping("/api/v1/billpay")
@RequiredArgsConstructor
@Tag(name = "Bill Payment (BBPS)", description = "Biller registration, bill fetch & payment — Nepal BBPS equivalent")
public class BillPaymentController {

    private final BillPaymentService billPaymentService;

    // ── Biller Discovery ──

    @GetMapping("/billers")
    @Operation(summary = "List all active billers")
    public ResponseEntity<List<Biller>> listBillers() {
        return ResponseEntity.ok(billPaymentService.listAllBillers());
    }

    @GetMapping("/billers/category/{category}")
    @Operation(summary = "List billers by category", description = "Categories: ELECTRICITY, WATER, TELECOM, GAS, BROADBAND, INSURANCE, EDUCATION, TAX, HOSPITAL")
    public ResponseEntity<List<Biller>> listByCategory(@PathVariable String category) {
        return ResponseEntity.ok(billPaymentService.listBillersByCategory(category.toUpperCase()));
    }

    @GetMapping("/billers/search")
    @Operation(summary = "Search billers by name")
    public ResponseEntity<List<Biller>> searchBillers(@RequestParam String name) {
        return ResponseEntity.ok(billPaymentService.searchBillers(name));
    }

    @PostMapping("/billers")
    @Operation(summary = "Register a new biller")
    public ResponseEntity<Biller> registerBiller(@RequestBody Map<String, Object> request) {
        Biller biller = billPaymentService.registerBiller(
                (String) request.get("billerId"),
                (String) request.get("billerName"),
                (String) request.get("category"),
                (String) request.get("subCategory"),
                (String) request.get("bankCode"),
                (String) request.get("settlementAccount"),
                Boolean.TRUE.equals(request.get("fetchSupported"))
        );
        return ResponseEntity.ok(biller);
    }

    // ── Bill Fetch & Pay ──

    @PostMapping("/fetch")
    @Operation(summary = "Fetch outstanding bill", description = "Fetches bill from biller's system for billers that support it")
    public ResponseEntity<?> fetchBill(@RequestBody Map<String, String> request) {
        Bill bill = billPaymentService.fetchBill(
                request.get("billerId"),
                request.get("customerIdentifier")
        );
        if (bill == null) {
            return ResponseEntity.ok(Map.of(
                    "message", "Ad-hoc biller — user must provide amount",
                    "adhoc", true
            ));
        }
        return ResponseEntity.ok(bill);
    }

    @PostMapping("/adhoc")
    @Operation(summary = "Create ad-hoc bill", description = "For billers that don't support bill fetch — user enters amount manually")
    public ResponseEntity<Bill> createAdHocBill(@RequestBody Map<String, Object> request) {
        Bill bill = billPaymentService.createAdHocBill(
                (String) request.get("billerId"),
                (String) request.get("customerIdentifier"),
                ((Number) request.get("amountPaisa")).longValue()
        );
        return ResponseEntity.ok(bill);
    }

    @PostMapping("/pay/{billId}")
    @Operation(summary = "Pay a bill via UPI", description = "Routes payment through standard UPI TransactionOrchestrator")
    public ResponseEntity<Bill> payBill(
            @PathVariable UUID billId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-PSP-ID", required = false) String pspId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        Bill paid = billPaymentService.payBill(
                billId,
                request.get("payerVpa"),
                pspId != null ? pspId : request.get("pspId"),
                request.get("deviceFingerprint"),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(paid);
    }

    @GetMapping("/history/{payerVpa}")
    @Operation(summary = "Get bill payment history for a user")
    public ResponseEntity<List<Bill>> getHistory(@PathVariable String payerVpa) {
        return ResponseEntity.ok(billPaymentService.getPaymentHistory(payerVpa));
    }
}
