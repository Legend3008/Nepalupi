package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * App lock preferences API.
 * Allows users to enable/disable app lock and choose lock type (MPIN, BIOMETRIC, PATTERN).
 */
@RestController
@RequestMapping("/api/v1/app-lock")
@RequiredArgsConstructor
public class AppLockController {

    private final UserRepository userRepository;

    private static final Set<String> VALID_LOCK_TYPES = Set.of("MPIN", "BIOMETRIC", "PATTERN");

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getPreferences(@PathVariable UUID userId) {
        User user = findUser(userId);
        Map<String, Object> prefs = new LinkedHashMap<>();
        prefs.put("appLockEnabled", Boolean.TRUE.equals(user.getAppLockEnabled()));
        prefs.put("appLockType", user.getAppLockType());
        prefs.put("availableTypes", VALID_LOCK_TYPES);
        return ResponseEntity.ok(prefs);
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<Map<String, Object>> enableAppLock(@PathVariable UUID userId,
                                                              @RequestParam String lockType) {
        if (!VALID_LOCK_TYPES.contains(lockType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid lock type. Valid: " + VALID_LOCK_TYPES));
        }

        User user = findUser(userId);
        user.setAppLockEnabled(true);
        user.setAppLockType(lockType.toUpperCase());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "appLockEnabled", true,
                "appLockType", lockType.toUpperCase(),
                "message", "App lock enabled with " + lockType
        ));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<Map<String, Object>> disableAppLock(@PathVariable UUID userId) {
        User user = findUser(userId);
        user.setAppLockEnabled(false);
        user.setAppLockType(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "appLockEnabled", false,
                "message", "App lock disabled"
        ));
    }

    @PutMapping("/{userId}/type")
    public ResponseEntity<Map<String, Object>> changeLockType(@PathVariable UUID userId,
                                                               @RequestParam String lockType) {
        if (!VALID_LOCK_TYPES.contains(lockType.toUpperCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid lock type. Valid: " + VALID_LOCK_TYPES));
        }

        User user = findUser(userId);
        if (!Boolean.TRUE.equals(user.getAppLockEnabled())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "App lock must be enabled first"));
        }

        user.setAppLockType(lockType.toUpperCase());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "appLockType", lockType.toUpperCase(),
                "message", "Lock type changed to " + lockType
        ));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
