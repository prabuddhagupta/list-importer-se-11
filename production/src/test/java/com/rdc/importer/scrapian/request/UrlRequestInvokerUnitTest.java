package com.rdc.importer.scrapian.request;

import java.io.IOException;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import com.rdc.importer.scrapian.model.ScrapianRequest;

public class UrlRequestInvokerUnitTest {
    private static final String content  = "<h1>Hello</h1>";
    private static final String redirectContent = "<h1>Redirected</h1>";
    private static final String encoding = "ISO-8859-1";
    private static final int port = 13333;
    private static final String url = "http://localhost:" + port;
    private static final String redirectPath = "/redirect";
    private static final String redirectUrl = url + redirectPath;
    private Server server;
    private UrlRequestInvoker invoker;

    @Before
    public void setUp() throws Exception {
        invoker = new UrlRequestInvoker();
        invoker.open(1000, 500, 0, false);
        server = new Server(port);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testOkInvoke() throws Exception {
        setHandler(SC_OK, null);
        final ScrapianRequest request = new ScrapianRequest(url);
        request.setType("post");
        assertTrimEquals(content, invoker.invoke(request, encoding));
        request.setType("get");
        assertTrimEquals(content, invoker.invoke(request, encoding));
    }

    @Test
    public void testGetRedirect() throws Exception {
        setHandler(SC_MOVED_PERMANENTLY, redirectUrl);
        final ScrapianRequest request = new ScrapianRequest(url);
        request.setType("get");
        assertTrimEquals(redirectContent, invoker.invoke(request, encoding));
    }

    @Test(expected=Exception.class)
    public void testPostRedirectNoLoc() throws Exception {
        setHandler(SC_MOVED_TEMPORARILY, null);
        invoker.invoke(getPostRequest(url), encoding);
    }

    @Test(expected=Exception.class)
    public void testPostBadCode() throws Exception {
        setHandler(SC_NOT_FOUND, redirectUrl);
        invoker.invoke(getPostRequest(url), encoding);
    }

    @Test
    public void testFullyQualifiedRedirect() throws Exception {
        setHandler(SC_MOVED_PERMANENTLY, redirectUrl);
        assertTrimEquals(redirectContent,
                         invoker.invoke(getPostRequest(url), encoding));
    }

    @Test
    public void testOffsetRedirect() throws Exception {
        setHandler(SC_MOVED_PERMANENTLY, redirectPath);
        assertTrimEquals(redirectContent,
                         invoker.invoke(getPostRequest(url), encoding));
    }

    @Test
    public void testJustPageRedirect() throws Exception {
        setHandler(SC_MOVED_TEMPORARILY, redirectPath.substring(1));
        assertTrimEquals(redirectContent,
                         invoker.invoke(getPostRequest(url + "/"), encoding));
    }

    @Test(expected=UnknownHostException.class)
    public void testUnknownHost() throws Exception {
        setHandler(SC_MOVED_TEMPORARILY, "http://blah");
        assertTrimEquals(redirectContent,
                         invoker.invoke(getPostRequest(url), encoding));
    }


    private void setHandler(final int status, final String location) {
        server.setHandler(new AbstractHandler() {
                public void handle
                    (final String target, final HttpServletRequest request,
                     final HttpServletResponse response, final int dispatch)
                        throws IOException, ServletException {
                    final String html;
                    if (request.getPathInfo().equals(redirectPath)) {
                        response.setStatus(SC_OK);
                        html = redirectContent;
                    } else {
                        response.setStatus(status);
                        html = content;
                    }
                    if (location != null) {
                        response.addHeader("location", location);
                    }
                    response.setContentType("text/html; charset=" + encoding);
                    response.getWriter().println(html);
                    ((Request)request).setHandled(true);
                }
            });
    }

    private ScrapianRequest getPostRequest(final String url) {
        final ScrapianRequest request = new ScrapianRequest(url);
        request.setType("post");
        return request;
    }

    private void assertTrimEquals(final Object s1, final Object s2) {
        Assert.assertEquals(s1.toString().trim(), s2.toString().trim());
    }
}
