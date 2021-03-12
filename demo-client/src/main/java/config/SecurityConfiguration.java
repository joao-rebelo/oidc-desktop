package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import security.AuthenticationProvider;
import security.AuthenticationProviderImpl;
import security.DesktopOAuth2AuthorizedClientExchangeFilterFunction;
import security.DesktopOAuth2LoginAuthenticationToken;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class SecurityConfiguration {
	//This is actually the default value, but just want to make sure its higher than the scheduler interval
	//This should help us not running into run conditions that a token gets temporarily expired while its getting refreshed
	private static final int REFRESH_CLOCK_SKEW = 60;
	private static final int REFRESH_SCHEDULER_INTERVAL = REFRESH_CLOCK_SKEW - 9;

	private static final String AUTHSERVICE_REGISTRATION_ID = "keycloak";
	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

	@Bean
	public ExchangeFilterFunction securityExchangeFilterFunction(OAuth2AuthorizedClientManager theAuthorizedClientManager) {
		var aExchangeFilterFunction = new DesktopOAuth2AuthorizedClientExchangeFilterFunction(theAuthorizedClientManager);

		aExchangeFilterFunction.setDefaultOAuth2AuthorizedClient(true);
		aExchangeFilterFunction.setDefaultClientRegistrationId(AUTHSERVICE_REGISTRATION_ID);

		return aExchangeFilterFunction;
	}

	//Defaults to a single thread on the pool, which is more than enough for refreshing a token if needed
	@Bean("OIDCTokenRefreshTaskScheduler")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setThreadNamePrefix("OIDCTokenRefreshTaskScheduler");
		return threadPoolTaskScheduler;
	}

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository theClientRegistrationRepository,
			TaskScheduler theTaskScheduler) {

		OAuth2AuthorizedClientService theOAuth2AuthorizedClientService = new InMemoryOAuth2AuthorizedClientService(theClientRegistrationRepository);

		var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.provider(aContext -> provider(aContext))
				.refreshToken(
						builder -> builder.accessTokenResponseClient(new RefreshTokenTokenResponseClient())
								.clockSkew(Duration.ofSeconds(REFRESH_CLOCK_SKEW)))
				.build();
		var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				theClientRegistrationRepository,
				theOAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		theTaskScheduler.scheduleAtFixedRate(() -> scheduleTokenRefresh(authorizedClientManager, theOAuth2AuthorizedClientService), Duration.ofSeconds(REFRESH_SCHEDULER_INTERVAL));

		return authorizedClientManager;
	}

	private OAuth2AuthorizedClient provider(OAuth2AuthorizationContext context) {
		if (context.getAuthorizedClient() != null) {
			return null;
		}

		var principal = context.getPrincipal();
		if (principal instanceof DesktopOAuth2LoginAuthenticationToken) {
			return new OAuth2AuthorizedClient(
					context.getClientRegistration(),
					context.getPrincipal().getName(),
					((DesktopOAuth2LoginAuthenticationToken) principal).getAccessToken(),
					((DesktopOAuth2LoginAuthenticationToken) principal).getRefreshToken());
		}
		return null;
	}

	@Bean
	public AuthenticationProvider authenticationProvider(ClientRegistrationRepository theClientRegistrationRepository) {
		ClientRegistration aClientRegistration = theClientRegistrationRepository.findByRegistrationId(AUTHSERVICE_REGISTRATION_ID);

		return new AuthenticationProviderImpl(aClientRegistration);
	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
		List<ClientRegistration> registrations = new ArrayList<>(
				OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
		return new InMemoryClientRegistrationRepository(registrations);
	}

	private class RefreshTokenTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> {
		DefaultRefreshTokenTokenResponseClient client = new DefaultRefreshTokenTokenResponseClient();

		@Override
		public OAuth2AccessTokenResponse getTokenResponse(OAuth2RefreshTokenGrantRequest theRefreshTokenGrantRequest) {
			var tokenResponse = client.getTokenResponse(theRefreshTokenGrantRequest);
			var authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication instanceof DesktopOAuth2LoginAuthenticationToken) {
				((DesktopOAuth2LoginAuthenticationToken) authentication).setAccessToken(tokenResponse.getAccessToken());
			}

			LOGGER.debug("OIDC Token has been refreshed");

			return tokenResponse;
		}

	}

	public void scheduleTokenRefresh(OAuth2AuthorizedClientManager theClientManager, OAuth2AuthorizedClientService theOAuth2AuthorizedClientService) {

		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null) {
			OAuth2AuthorizedClient authorizedClient = theOAuth2AuthorizedClientService.loadAuthorizedClient(AUTHSERVICE_REGISTRATION_ID, authentication.getName());

			if (authorizedClient != null) {
				OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withAuthorizedClient(authorizedClient).principal(authentication).build();

				LOGGER.debug("Checking if OIDC access token needs refresh");
				theClientManager.authorize(request);
			}
		}

	}
}
