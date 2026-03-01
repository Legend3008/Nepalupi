package np.com.nepalupi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VpaResolveRequest {

    @NotBlank(message = "VPA address is required")
    private String vpa;
}
