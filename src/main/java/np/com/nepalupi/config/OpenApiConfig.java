package np.com.nepalupi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 documentation configuration for Nepal UPI Switch.
 * <p>
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * API Docs:   http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nepalUpiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nepal UPI Payment Switch API")
                        .version("1.0.0")
                        .description("""
                                Nepal's Unified Payment Interface (UPI) — a real-time payment switch
                                modeled on India's NPCI architecture, operated via NCHL (Nepal Clearing House Ltd).
                                
                                ## Features
                                - P2P and P2M instant payments
                                - Collect (pull) payments
                                - Mandate (recurring) payments
                                - VPA resolution and management
                                - Merchant onboarding with QR codes
                                - Balance enquiry
                                - Dispute resolution (UDIR)
                                - NRB compliance reporting
                                
                                ## Authentication
                                PSP API requests must include:
                                - `X-PSP-ID`: Registered PSP identifier
                                - `X-Timestamp`: ISO-8601 timestamp
                                - `X-Signature`: HMAC-SHA256 signature
                                """)
                        .contact(new Contact()
                                .name("Nepal UPI Switch Team")
                                .email("admin@nepalupi.np"))
                        .license(new License()
                                .name("Nepal Rastra Bank License")
                                .url("https://nrb.org.np")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Development"),
                        new Server().url("https://api.nepalupi.np").description("Production")))
                .components(new Components()
                        .addSecuritySchemes("pspAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-PSP-ID")
                                .description("PSP identifier header"))
                        .addSecuritySchemes("hmacSignature", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Signature")
                                .description("HMAC-SHA256 signature")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("pspAuth")
                        .addList("hmacSignature"));
    }
}
