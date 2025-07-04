package bpmnProject.akon.bpmnJavaBackend.Auth;

import bpmnProject.akon.bpmnJavaBackend.User.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    private User user;

    @JsonProperty("expires_in")
    private Long expiresIn;

    public String getToken() {
        return accessToken;
    }
}