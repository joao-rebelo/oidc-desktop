package security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import net.minidev.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.*;

public class AuthenticationProviderImpl implements AuthenticationProvider {
	private final ClientRegistration clientRegistration;

	public AuthenticationProviderImpl(ClientRegistration theClientRegistration) {
		clientRegistration = theClientRegistration;
	}

	@Override
	public Authentication registerAuthentication(String theTokenEndpointResponse, String theUserInfoResponse) {
		ObjectMapper objMapper = new ObjectMapper();

		TypeReference<Map<String, Object>> type = new TypeReference<Map<String, Object>>() {//
		};

		Map<String, Object> jsonMap = null;
		try {
			jsonMap = objMapper.readValue(theTokenEndpointResponse, type);
		}
		catch (JsonProcessingException theJsonProcessingException) {
			throw new RuntimeException("The json from the authorization code flow is in incorrect format", theJsonProcessingException);
		}

		OIDCTokenResponse parsedOidcToken = null;
		try {
			var tokenResponse = OIDCTokenResponseParser.parse(new JSONObject(jsonMap));
			if (tokenResponse.indicatesSuccess()) {
				parsedOidcToken = (OIDCTokenResponse) tokenResponse;
			}
			else {
				//This (and other parse exceptions) should not happen since the success is validated on the login browser integration
				throw new RuntimeException("Authorization code flow response indicates insuccess");
			}
		}
		catch (ParseException theParseException) {
			throw new RuntimeException("Error parsing the json from the authorization code flow", theParseException);
		}

		OAuth2AccessTokenResponse accessTokenResponse = oauth2AccessTokenResponse(parsedOidcToken);

		var nimbusIdToken = parsedOidcToken.getOIDCTokens().getIDToken();

		OAuth2AccessToken accessToken = accessTokenResponse.getAccessToken();

		OidcIdToken idToken = null;
		try {
			Map<String, Object> claims = new HashMap<>(nimbusIdToken.getJWTClaimsSet().getClaims());

			JWT accessTokenJwt = JWTParser.parse(accessToken.getTokenValue());

			idToken = new OidcIdToken(
					nimbusIdToken.getParsedString(),
					accessToken.getIssuedAt(),
					accessToken.getExpiresAt(),
					claims);
		}
		catch (java.text.ParseException e) {
			throw new RuntimeException("Error parsing the ID Token claims", e);
		}

		DefaultOidcUser user = new DefaultOidcUser(parseAuthorities(idToken, accessToken), idToken);

		DesktopOAuth2LoginAuthenticationToken authenticationResult = new DesktopOAuth2LoginAuthenticationToken(
				clientRegistration,
				user,
				user.getAuthorities(),
				accessTokenResponse.getAccessToken(),
				accessTokenResponse.getRefreshToken());

		provideSecurityContext(authenticationResult);

		return authenticationResult;
	}

	private Set<GrantedAuthority> parseAuthorities(OidcIdToken theIdToken, OAuth2AccessToken theAccessToken) {
		Set<GrantedAuthority> allAuthorities = new LinkedHashSet<>();

		for (String authority : theAccessToken.getScopes()) {
			allAuthorities.add(new SimpleGrantedAuthority("SCOPE_" + authority));
		}
		return allAuthorities;
	}

	private OAuth2AccessTokenResponse oauth2AccessTokenResponse(AccessTokenResponse accessTokenResponse) {
		AccessToken accessToken = accessTokenResponse.getTokens().getAccessToken();
		OAuth2AccessToken.TokenType accessTokenType = null;
		if (OAuth2AccessToken.TokenType.BEARER.getValue()
				.equalsIgnoreCase(accessToken.getType().getValue())) {
			accessTokenType = OAuth2AccessToken.TokenType.BEARER;
		}
		long expiresIn = accessToken.getLifetime();

		Set<String> scopes = accessToken.getScope() == null ? Collections.emptySet() : new LinkedHashSet<>(accessToken.getScope().toStringList());

		String refreshToken = null;
		if (accessTokenResponse.getTokens().getRefreshToken() != null) {
			refreshToken = accessTokenResponse.getTokens().getRefreshToken().getValue();
		}

		Map<String, Object> additionalParameters = new LinkedHashMap<>(accessTokenResponse.getCustomParameters());

		return OAuth2AccessTokenResponse.withToken(accessToken.getValue())
				.tokenType(accessTokenType)
				.expiresIn(expiresIn)
				.scopes(scopes)
				.refreshToken(refreshToken)
				.additionalParameters(additionalParameters)
				.build();
	}

	private void provideSecurityContext(Authentication theAuthentication) {
		var securityContext = SecurityContextHolder.createEmptyContext();

		securityContext.setAuthentication(theAuthentication);
		SecurityContextHolder.setContext(securityContext);
	}
}
