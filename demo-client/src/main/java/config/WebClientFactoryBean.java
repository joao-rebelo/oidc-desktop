package config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

public class WebClientFactoryBean implements FactoryBean<WebClient> {
	private String baseUrl;
	private ClientHttpConnector connector;
	private ExchangeFilterFunction exchangeFilterFunction;

	public void setBaseUrl(String theBaseUrl) {
		baseUrl = theBaseUrl;
	}

	public void setConnector(ClientHttpConnector theConnector) {
		connector = theConnector;
	}

	public void setExchangeFilterFunction(ExchangeFilterFunction theExchangeFilterFunction) {
		exchangeFilterFunction = theExchangeFilterFunction;
	}

	@Override
	public WebClient getObject() {
		Builder aWebClientBuilder = WebClient.builder()
				.baseUrl(baseUrl);

		if (connector != null) {
			aWebClientBuilder.clientConnector(connector);
		}

		if (exchangeFilterFunction != null) {
			aWebClientBuilder.filter(exchangeFilterFunction);
		}

		return aWebClientBuilder.build();
	}

	@Override
	public Class<?> getObjectType() {
		return WebClient.class;
	}
}
