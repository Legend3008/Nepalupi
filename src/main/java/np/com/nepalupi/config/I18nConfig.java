package np.com.nepalupi.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Internationalization (i18n) configuration for NUPI.
 * Supports Nepali (ne), English (en), Hindi (hi), Maithili (mai), Bhojpuri (bho).
 * Default locale: Nepali (ne).
 */
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Refresh every hour
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(new Locale("ne")); // Nepali default
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(new Locale("ne")); // Nepali default
        return resolver;
    }
}
