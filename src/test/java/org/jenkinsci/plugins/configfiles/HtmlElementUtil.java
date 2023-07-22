package org.jenkinsci.plugins.configfiles;

import java.io.IOException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.WebClientUtil;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;

public class HtmlElementUtil {
  public HtmlElementUtil() {
  }

  public static void clickDialogOkButton(HtmlElement element, HtmlElement document) throws IOException {
    if (element != null) {
      boolean var6 = false;

      try {
        var6 = true;
        element.click();
        var6 = false;
      } finally {
        if (var6) {
          WebClient var4 = element.getPage().getWebClient();
          WebClientUtil.waitForJSExec(var4);
        }
      }

      WebClient webClient = element.getPage().getWebClient();
      WebClientUtil.waitForJSExec(webClient);
      HtmlButton confirmButton = document.getOneHtmlElementByAttribute("button", "data-id", "ok");
      confirmButton.click();
    }
  }
}
