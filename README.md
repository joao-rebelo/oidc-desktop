# OIDC Desktop

This is a runnable example of how to achieve Desktop Spring OIDC support.
The tokens are retrieved with an OIDC Authentication Code Flow, and the response is then provided to the AuthenticationProviderImpl class

We've created / cloned a new OAuth2LoginAuthenticationToken which doesn't require the exchange object, and on AuthenticationProviderImpl we use the same Nimbus code that is used internally on Spring to parse the OIDC responses / tokens.

Since this is a Desktop application we've set the SecurityContextHolder to work in GLOBAL mode.
Therefor we needed to create / clone the DesktopOAuth2AuthorizedClientExchangeFilterFunction to change the getAuthentication method (not use the Thread Local)

The application is able to refresh its token if needed.
Making a back-channel logout when the application closes isn't hard, but depends on the IdP specifics.

# To execute

* Run the server

1. Build the images into local docker.
   1. cd server
   2. gradle jibDockerBuild
2. Unzip the dbFolder.zip
3. At server/demo-compose change the device from postgres_data volume to the unzipped dbFolder path
4. docker-compose up. Will use ports
   1. 80 for the Login pages, 
   2. 8080 for Keycloak
   3. 8081 for Demo Server

* Run the client

1. Get a license from jxBrowser
   * An evaluation license can can be fetched here: https://www.teamdev.com/jxbrowser#evaluate
2. Set the license at the client application.yml
   1. Copy the demo-client\src\main\resources\application.yml.template to demo-client\src\main\resources\application.yml
   2. Replace the <yourLicenseHere!!> with your license
   * If you change any port from the docker-compose file it has to be adapted here 
3. Build the client: 
   1. cd demo-client
   2. gradle build
3. Run the client:
   1. java -jar demo-client\build\libs\demo-client-1.0-SNAPSHOT.jar
4. Login with the demo account: test / test   

# Components

## Demo-client 
	Contains the Swing application that performs the login and calls the server.
	Uses a jxBrowser to run the Authorization Code flow with the IdP.
	Uses the web pages from the loginPage container to extract the tokens to Java.

## Demo-server
	This project contains the Resource Server of an hello world which replies with the "preferred_username" claim from the request.
	
## loginPage
	This project builds a NGINX-based webserver serving the login and redirect webpages. These interact with OIDC redirects and the Demo-client will use them from the jxBrowser
	
## demo-compose
	Example compose file to launch the server
	
## Keycloak
	The dump on this repository will use an admin / admin master user.
	It defines an "example" realm, with an "exampleClient" client.
	It contains a single user: test / test. This can be used to login from the Demo-client