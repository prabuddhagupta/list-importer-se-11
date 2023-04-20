package test;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;


public class HtmlUnit {

  public static void main(String[] args) throws InterruptedException {
    System.out.println(FxWebClient.getData("https://crimestoppers-uk.org/most-wanted/?Page=1"));
  }


  public static class FxWebClient extends Application {
    private static CountDownLatch latch_ = new CountDownLatch(1);
    private static FxWebClient.Browser browser;
    public FxWebClient(){}
    private static void prepare() {
      if (browser == null) {
        Thread thread = new Thread(() -> {
          FxWebClient.launch(FxWebClient.class, null);
        });
        thread.setDaemon(true);
        thread.start();
      }
    }

    @Override
    public void start(Stage stage) {
      browser = new FxWebClient.Browser();
      Scene scene = new Scene(browser, 1000, 700, Color.WHITE);
      stage.setScene(scene);
      //stage.show();
      latch_.countDown();
    }

    class Browser extends Region {
      void addView(Node webView) {
        getChildren().add(webView);
      }

      void remove(Node view) {
        getChildren().remove(view);
      }
    }

    public static String getData(String url) throws InterruptedException {
      prepare();
      latch_.await();
      CountDownLatch latch = new CountDownLatch(1);
      final String[] data = {""};

      Toolkit.getToolkit().defer(() -> {
        WebView webView = new WebView();
        browser.addView(webView);
        WebEngine webEngine = webView.getEngine();
        webEngine.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36");
        webEngine.load(url);
        webEngine.getLoadWorker().stateProperty().addListener(
            (ov, oldState, newState) -> {
              if (newState == Worker.State.SUCCEEDED) {
                try {
                  data[0] = docToString(webEngine.getDocument());
                } catch (TransformerException e) {
                  e.printStackTrace();
                }
                latch.countDown();
                browser.remove(webView);
              }
            });
      });

      latch.await();
      return data[0];
    }

    private static String docToString(Document doc) throws TransformerException {
      DOMSource domSource = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.transform(domSource, result);

      return writer.toString();
    }

    //TODO: Activate these
    //- 1. Add method/closure to return response cookies
    //- 2. Add custom content filtering for lighter loading (ie. do not load img/ico/non-js contents)
    //- 3. Convert the invoker and other methods into instance based rather than static-method based
    //- 4. Use close method to destroy the hidden window

    //ie. https://stackoverflow.com/questions/12524580/disable-automatic-image-loading-in-webview
  }
}

