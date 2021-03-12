# DesktopSpringSecurity

This is a runnable example of how to achieve Desktop Spring OIDC support.
The tokens are retrieved with an OIDC Authentication Code Flow, and the response is then provided to the AuthenticationProviderImpl class

We've created / cloned a new OAuth2LoginAuthenticationToken which doesn't require the exchange object, and on AuthenticationProviderImpl we use the same Nimbus code that is used internally on Spring to parse the OIDC responses / tokens.

Since this is a Desktop application we've set the SecurityContextHolder to work in GLOBAL mode.
Therefor we needed to create / clone the DesktopOAuth2AuthorizedClientExchangeFilterFunction to change the getAuthentication method (not use the Thread Local)

The application is able to refresh its token if needed

# Components

## Demo-client 
	Contains the Swing application that performs the login and calls the server.
	For this project to run its needed to have a license from jxBrowser. You may get an evaluation license from here: https://www.teamdev.com/jxbrowser#evaluate
	This license should then be set at the application.yml.template and rename it to application.yml
	
## Server
	Multi server gradle project that produces all containers for the running example. To build the containers for local Docker run: gradle jibDockerBuild

### Demo-server
	This project contains the Resource Server of an hello world which replies with the "preferred_username" claim from the request.
	
### loginPage
	This project builds a NGINX-based webserver serving the login and redirect webpages. These interact with OIDC redirects and the Demo-client will use them from the jxBrowser
	
### demo-compose
	Example compose file to launch the server
	It requires a DB folder for Keycloak definitions. You may unzip the dump on this repository
	
## Keycloak
	The dump on this repository will use an admin / admin master user.
	It defines an "example" realm, with an "exampleClient" client.
	It contains a single user: test / test. This can be used to login from the Demo-client