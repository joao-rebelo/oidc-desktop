package browser;

import com.teamdev.jxbrowser.js.JsAccessible;

import java.util.function.BiConsumer;

public class LoginBridge {
    private final BiConsumer<String, String> loginHandler;

    public LoginBridge(BiConsumer<String, String> loginHandler) {
        this.loginHandler = loginHandler;
    }

    /**
     * Called from the browser to treat the tokens
     *
     * @param theCodeAuthorizationFlowResponse the JSON stringify object response
     * @param theUserInfoResponse              the JSON stringify object response
     */
    @JsAccessible
    public void setTokens(String theCodeAuthorizationFlowResponse, String theUserInfoResponse) {
        System.out.println("The Code Authorization Flow response: " + theCodeAuthorizationFlowResponse);
        System.out.println("The User Info response: " + theUserInfoResponse);
        loginHandler.accept(theCodeAuthorizationFlowResponse, theUserInfoResponse);
    }
}
