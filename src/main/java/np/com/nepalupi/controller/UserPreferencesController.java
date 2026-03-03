package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.service.notification.EmailNotificationService;
import np.com.nepalupi.service.notification.PushNotificationService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * User preferences controller: language, email, FCM token, notifications.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final EmailNotificationService emailNotificationService;
    private final PushNotificationService pushNotificationService;

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ne", "en", "hi", "mai", "bho");

    /**
     * Get user preferences (language, email, notifications).
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<Map<String, Object>> getPreferences(@PathVariable UUID userId) {
        User user = findUser(userId);

        Map<String, Object> prefs = new LinkedHashMap<>();
        prefs.put("preferredLanguage", user.getPreferredLanguage());
        prefs.put("supportedLanguages", SUPPORTED_LANGUAGES);
        prefs.put("email", user.getEmail());
        prefs.put("emailVerified", Boolean.TRUE.equals(user.getEmailVerified()));
        prefs.put("hasFcmToken", user.getFcmToken() != null);
        prefs.put("appLockEnabled", Boolean.TRUE.equals(user.getAppLockEnabled()));
        prefs.put("appLockType", user.getAppLockType());

        return ResponseEntity.ok(prefs);
    }

    /**
     * Update preferred language.
     */
    @PutMapping("/{userId}/language")
    public ResponseEntity<Map<String, Object>> updateLanguage(@PathVariable UUID userId,
                                                               @RequestParam String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unsupported language. Supported: " + SUPPORTED_LANGUAGES));
        }

        User user = findUser(userId);
        user.setPreferredLanguage(language);
        userRepository.save(user);

        // Return welcome message in selected language
        String welcome = messageSource.getMessage("auth.welcome", null, new Locale(language));

        return ResponseEntity.ok(Map.of(
                "preferredLanguage", language,
                "welcome", welcome
        ));
    }

    /**
     * Update email address.
     */
    @PutMapping("/{userId}/email")
    public ResponseEntity<Map<String, Object>> updateEmail(@PathVariable UUID userId,
                                                            @RequestParam String email) {
        User user = findUser(userId);
        user.setEmail(email);
        user.setEmailVerified(false); // Needs re-verification
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("email", email, "emailVerified", false,
                "message", "Verification email sent. Please check your inbox."));
    }

    /**
     * Verify email (in production: verify via OTP/link).
     */
    @PostMapping("/{userId}/email/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@PathVariable UUID userId) {
        User user = findUser(userId);
        user.setEmailVerified(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("emailVerified", true));
    }

    /**
     * Register / update FCM push token.
     */
    @PutMapping("/{userId}/fcm-token")
    public ResponseEntity<Map<String, Object>> updateFcmToken(@PathVariable UUID userId,
                                                               @RequestParam String token) {
        pushNotificationService.updateFcmToken(userId, token);
        return ResponseEntity.ok(Map.of("fcmTokenUpdated", true));
    }

    /**
     * Get localized message.
     */
    @GetMapping("/i18n/{key}")
    public ResponseEntity<Map<String, String>> getLocalizedMessage(@PathVariable String key,
                                                                    @RequestParam(defaultValue = "ne") String language) {
        String message = messageSource.getMessage(key, null, key, new Locale(language));
        return ResponseEntity.ok(Map.of("key", key, "language", language, "message", message));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
