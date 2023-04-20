import java.io.IOException;
import java.net.MalformedURLException;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class MyHtmlUnit {

    public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {

        //initialize a headless browser
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6);

//        //configuring options
//        webClient.getOptions().setUseInsecureSSL(true);
//        webClient.getOptions().setCssEnabled(false);
//        webClient.getOptions().setJavaScriptEnabled(false);
//        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
//        webClient.getOptions().setThrowExceptionOnScriptError(false);
//

        //fetching the web page
        HtmlPage page = webClient.getPage("https://www.reddit.com/r/scraping/");

        //selecting all headings
        DomNodeList<DomNode> headings = page.querySelectorAll("h3._eYtD2XCVieq6emjKBH3m");

        //iterating and extracting
        for (DomNode content: headings) {
            System.out.println(content.asText());
        }
    }
}