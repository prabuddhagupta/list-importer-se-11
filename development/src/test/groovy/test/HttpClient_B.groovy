package test

class HttpClient_B {
    HttpURLConnection httpURLConnection = null
    CookieHandler cookieHandler
    String postParams
    String userAgent
    String acceptLanguage
    String accept
    String url;
    def content = null

    HttpClient_B(String url){
        this.url = url
    }

    def  getContentData(){
        try {
            this.httpURLConnection = new URL(getUrl()).openConnection()
            this.httpURLConnection.setRequestProperty("User-Agent",getUserAgent())
            this.httpURLConnection.setRequestProperty("Accept-Language",getAcceptLanguage())
            this.httpURLConnection.setRequestProperty("Accept",getAccept())
            Scanner scanner = new Scanner(httpURLConnection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next()

        }catch (Exception ex){
            ex.printStackTrace()
        }
        return content
    }

    def sendPost(){
        try{
            CookieManager manager = new CookieManager()
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(manager)
            this.httpURLConnection = new URL(getUrl()).openConnection()
            this.httpURLConnection.setRequestMethod("POST");
            this.httpURLConnection.setRequestProperty("User-Agent",getUserAgent())
            this.httpURLConnection.setRequestProperty("Accept-Language",getAcceptLanguage())
            this.httpURLConnection.setRequestProperty("Accept",getAccept())

            this.httpURLConnection.setDoOutput(true)
            OutputStream os = this.httpURLConnection.getOutputStream()
            os.write(getPostParams().getBytes())
            os.flush()
            os.close()

            int responseCode = this.httpURLConnection.getResponseCode();
            println("Post Response code::" + responseCode)

            if (responseCode == HttpURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(new InputStreamReader( this.httpURLConnection.getInputStream()))
                String inputLine;
                StringBuffer response = new StringBuffer()
                while ((inputLine = br.readLine()) != null){
                    response.append("\n")
                    response.append(inputLine)
                }
                br.close()
                return response.toString()
            }else {
                println("Post Resquest not worked")
            }

        }catch (Exception ex){
            ex.printStackTrace()
        }
    }

    String getUrl() {
        return url
    }

    void setUrl(String url) {
        this.url = url
    }

    def getPostParams() {
        return postParams
    }

    void setPostParams(List postParams) {
        this.postParams = postParams
    }

    String getUserAgent() {
        return userAgent
    }

    void setUserAgent(String userAgent) {
        this.userAgent = userAgent
    }

    String getConn() {
        return conn
    }

    void setConn(String conn) {
        this.conn = conn
    }

    String getAcceptLanguage() {
        return acceptLanguage
    }

    void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage
    }

    String getAccept() {
        return accept
    }

    void setAccept(String accept) {
        this.accept = accept
    }

    def setHeaders(String accept, String acceptLanguage, String userAgent) {
        this.accept = accept
        this.acceptLanguage = acceptLanguage
        this.userAgent = userAgent

    }
}
