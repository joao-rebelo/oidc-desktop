package security;

import org.springframework.security.core.Authentication;

public interface AuthenticationProvider {
	Authentication registerAuthentication(String theTokenEndpointResponse, String theUserInfoResponse);
}
