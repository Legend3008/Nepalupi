package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.service.fraud.AccountFreezeService;
import np.com.nepalupi.service.notification.PushNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin dashboard controller for operations management.
 * Provides tools for user management, fraud ops, VPA management, and system stats.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final AccountFreezeService accountFreezeService;
    private final PushNotificationService pushNotificationService;

    // ── User Management ──

    @GetMapping("/users")
    public ResponseEntity<Page<User>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userRepository.findAll(pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("userId", userId, "isActive", true));
    }

    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("userId", userId, "isActive", false));
    }

    // ── VPA Freeze / Blacklist ──

    @PostMapping("/users/{userId}/freeze")
    public ResponseEntity<Map<String, Object>> freezeUser(@PathVariable UUID userId,
                                                           @RequestParam String reason,
                                                           @RequestParam String adminId) {
        accountFreezeService.freezeAccount(userId, "ADMIN[" + adminId + "]: " + reason);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "FROZEN", "reason", reason, "admin", adminId));
    }

    @PostMapping("/users/{userId}/unfreeze")
    public ResponseEntity<Map<String, Object>> unfreezeUser(@PathVariable UUID userId,
                                                             @RequestParam String reason,
                                                             @RequestParam String adminId) {
        accountFreezeService.unfreezeAccount(userId, adminId, reason);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "ACTIVE", "reason", reason, "admin", adminId));
    }

    @PutMapping("/users/{userId}/blacklist")
    public ResponseEntity<Map<String, Object>> blacklistUser(@PathVariable UUID userId,
                                                              @RequestParam String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(false);
        user.setIsFrozen(true);
        user.setFreezeReason("BLACKLISTED: " + reason);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "BLACKLISTED", "reason", reason));
    }

    @PutMapping("/users/{userId}/whitelist")
    public ResponseEntity<Map<String, Object>> whitelistUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(true);
        user.setIsFrozen(false);
        user.setFreezeReason(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "WHITELISTED"));
    }

    // ── KYC Management ──

    @PutMapping("/users/{userId}/kyc")
    public ResponseEntity<Map<String, Object>> updateKycStatus(@PathVariable UUID userId,
                                                                @RequestParam String kycStatus,
                                                                @RequestParam String kycLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setKycStatus(kycStatus);
        user.setKycLevel(kycLevel);
        // Adjust daily limit based on KYC level
        long limit = switch (kycLevel.toUpperCase()) {
            case "FULL" -> 10000000L; // Rs 1,00,000
            case "MIN" -> 5000000L;   // Rs 50,000
            default -> 2500000L;       // Rs 25,000
        };
        user.setDailyLimitPaisa(limit);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "kycStatus", kycStatus,
                "kycLevel", kycLevel,
                "dailyLimitRs", limit / 100.0
        ));
    }

    // ── System Stats ──

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long frozenUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsFrozen())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("frozenUsers", frozenUsers);
        stats.put("kycCompletedUsers", userRepository.findAll().stream()
                .filter(u -> "VERIFIED".equals(u.getKycStatus())).count());

        return ResponseEntity.ok(stats);
    }

    // ── Notifications ──

    @PostMapping("/notify/{userId}")
    public ResponseEntity<Map<String, Object>> sendAdminNotification(@PathVariable UUID userId,
                                                                      @RequestParam String title,
                                                                      @RequestParam String body) {
        pushNotificationService.notifyPromo(userId, title, body);
        return ResponseEntity.ok(Map.of("sent", true, "userId", userId.toString()));
    }
}
