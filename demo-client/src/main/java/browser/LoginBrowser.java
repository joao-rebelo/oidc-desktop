package browser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.js.ConsoleMessage;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.view.swing.BrowserView;

import java.util.function.BiConsumer;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;

public class LoginBrowser {

    private String issuerUrl;
    private String realm;
    private String client;

    BiConsumer<String, String> authenticationHandler;

    private Browser browser;

    public LoginBrowser (String issuerUrl, String realm, String client, BiConsumer<String, String> authenticationHandler) {
        this.issuerUrl = issuerUrl;
        this.realm = realm;
        this.client = client;
        this.authenticationHandler = authenticationHandler;
        initBrowser();
    }

    private void initBrowser() {
        // Creating and running Chromium engine
        Engine engine = Engine.newInstance(
                EngineOptions.newBuilder(HARDWARE_ACCELERATED).build());

        browser = engine.newBrowser();

        // Creating Swing component for rendering web content
        // loaded in the given Browser instance
        BrowserView view = BrowserView.newInstance(browser);

        browser.set(InjectJsCallback.class, params -> {
            JsObject window = params.frame().executeJavaScript("window");
            window.putProperty("issuerUrl", issuerUrl);
            window.putProperty("realm", realm);
            window.putProperty("client", client);
            window.putProperty("loginBridge", new LoginBridge((tokenEndpointResponse, userInfoResponse) -> {
                System.out.println(tokenEndpointResponse);
                System.out.println(userInfoResponse);

                authenticationHandler.accept(tokenEndpointResponse, userInfoResponse);
            }));
            return InjectJsCallback.Response.proceed();
        });

        browser.on(ConsoleMessageReceived.class, event -> {
            ConsoleMessage consoleMessage = event.consoleMessage();
            System.out.println("Level: " + consoleMessage.level() + ", message: " + consoleMessage.message());
        });
    }

    public BrowserView getView() {
        return BrowserView.newInstance(browser);
    }

    public void loadUrl(String url) {
        browser.navigation().loadUrl(url);
    }
}
