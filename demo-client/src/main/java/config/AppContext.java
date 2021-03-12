package config;

import org.eclipse.jetty.client.HttpClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.ClientHttpConnectorAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication(exclude = {ClientHttpConnectorAutoConfiguration.class,
		ReactiveOAuth2ClientAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Configuration
@Import({
		SecurityConfiguration.class
})
public class AppContext {

	@Bean
	public ClientHttpConnector clientHttpConnector() {
		HttpClient httpClient = new HttpClient();
		return new JettyClientHttpConnector(httpClient);
	}

	@Bean
	public FactoryBean<WebClient> webClient(
			@Value(value = "${example.server.url}") String theServerBaseUrl,
			ClientHttpConnector theClientHttpConnector,
			ExchangeFilterFunction theExchangeFilterFunction) {
		var aWebClientFactoryBean = new WebClientFactoryBean();

		aWebClientFactoryBean.setBaseUrl(theServerBaseUrl);
		aWebClientFactoryBean.setConnector(theClientHttpConnector);
		aWebClientFactoryBean.setExchangeFilterFunction(theExchangeFilterFunction);

		return aWebClientFactoryBean;
	}
}
