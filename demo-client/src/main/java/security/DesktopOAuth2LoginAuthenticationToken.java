/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * Copy of {org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken without the exchange object
 */
public class DesktopOAuth2LoginAuthenticationToken extends AbstractAuthenticationToken {
	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;
	private OidcUser principal;
	private ClientRegistration clientRegistration;
	private OAuth2AccessToken accessToken;
	private OAuth2RefreshToken refreshToken;

	/**
	 * This constructor should be used when the Access Token Request/Response is complete,
	 * which indicates that the Authorization Code Grant flow has fully completed
	 * and OAuth 2.0 Login has been achieved.
	 *
	 * @param theClientRegistration the client registration
	 * @param thePrincipal the user {@code Principal} registered with the OAuth 2.0 Provider
	 * @param theAuthorities the authorities granted to the user
	 * @param theAccessToken the access token credential
	 * @param theRefreshToken the refresh token credential
	 */
	public DesktopOAuth2LoginAuthenticationToken(
			ClientRegistration theClientRegistration,
			OidcUser thePrincipal,
			Collection<? extends GrantedAuthority> theAuthorities,
			OAuth2AccessToken theAccessToken,
			OAuth2RefreshToken theRefreshToken) {
		super(theAuthorities);
		Assert.notNull(theClientRegistration, "clientRegistration cannot be null");
		Assert.notNull(thePrincipal, "principal cannot be null");
		Assert.notNull(theAccessToken, "accessToken cannot be null");
		this.clientRegistration = theClientRegistration;
		this.principal = thePrincipal;
		this.accessToken = theAccessToken;
		this.refreshToken = theRefreshToken;
		this.setAuthenticated(true);
	}

	@Override
	public OidcUser getPrincipal() {
		return this.principal;
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	/**
	 * Returns the {@link ClientRegistration client registration}.
	 *
	 * @return the {@link ClientRegistration}
	 */
	public ClientRegistration getClientRegistration() {
		return this.clientRegistration;
	}

	/**
	 * Returns the {@link OAuth2AccessToken access token}.
	 *
	 * @return the {@link OAuth2AccessToken}
	 */
	public OAuth2AccessToken getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(OAuth2AccessToken theAccessToken) {
		accessToken = theAccessToken;
	}

	/**
	 * Returns the {@link OAuth2RefreshToken refresh token}.
	 *
	 * @since 5.1
	 * @return the {@link OAuth2RefreshToken}
	 */
	public OAuth2RefreshToken getRefreshToken() {
		return this.refreshToken;
	}

}
