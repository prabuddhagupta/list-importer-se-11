package test

/**
 * Created by Zawad on 3/16/2017.
 */
class HttpClient {
     private URL url
     private HttpURLConnection connection

    public HttpClient(){

    }

    public String getURLData(String inputUrl){
        String htmlData = ""

        try {
            url = new URL(inputUrl)
            connection =(HttpURLConnection) url.openConnection()

            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
            }


        } catch (IOException ioe) {

            ioe.printStackTrace()
        }
      return htmlData

    }

    public String sendPost(String inputUrl,Map<?,?> inputData){
        url =new URL(inputUrl)
        connection =(HttpURLConnection) url.openConnection()

        String htmlData=""
        String postData=""

        connection.setRequestMethod("POST")
//
//        connection.setRequestProperty("User-Agent","Mozilla/5.0")
//        String value = connection.getRequestProperty("User-Agent")
//
//        println(value)

        inputData.each {
            postData+=it.key
            postData+="="
            postData+=it.value
            postData+="&"
        }

        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(postData.getBytes());
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode()



        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
                htmlData+="\n"
            }

        }else{
            println("Post did not work")
        }

       return htmlData
    }

    public String sendGet(String inputUrl){
        url =new URL(inputUrl)
        connection =(HttpURLConnection) url.openConnection()

        String htmlData=""

        connection.setRequestMethod("GET")

        int responseCode = connection.getResponseCode()


      /*  if(responseCode == HttpURLConnection.HTTP_OK){*/
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
                htmlData+="\n"
            }

       /* }else{
            println("Get did not work")
        }*/


        return htmlData

    }

    public String sendGet(String inputUrl,Map<?,?> inputHeaders){
        url =new URL(inputUrl)
        connection =(HttpURLConnection) url.openConnection()

        String htmlData=""

        connection.setRequestMethod("GET")

        inputHeaders.each {
            connection.setRequestProperty(it.key,it.value)
        }

        int responseCode = connection.getResponseCode()


        /*if(responseCode == HttpURLConnection.HTTP_OK){*/
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
                htmlData+="\n"
            }

       /* }
        else{
            println("Get did not work")
        }*/


        return htmlData

    }


    public String sendPost(String inputUrl,Map<?,?> inputData,Map<?,?> inputHeaders){
        url =new URL(inputUrl)
        connection =(HttpURLConnection) url.openConnection()

        String htmlData=""
        String postData=""

        connection.setRequestMethod("POST")

       inputHeaders.each {
            connection.setRequestProperty(it.key,it.value)
       }

        inputData.each {
            postData+=it.key
            postData+="="
            postData+=it.value
            postData+="&"
        }

        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(postData.getBytes());
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode()

        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
                htmlData+="\n"
            }

        }else{
            println("Post did not work")
        }

        return htmlData


    }

    public String sendPostwithHeader(String inputUrl,Map<?,?> inputHeaders){
        url =new URL(inputUrl)
        connection =(HttpURLConnection) url.openConnection()

        String htmlData=""
        String postData=""

        connection.setRequestMethod("POST")

        inputHeaders.each {
            connection.setRequestProperty(it.key,it.value)
        }



        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        os.write(postData.getBytes());
        os.flush();
        os.close();

        int responseCode = connection.getResponseCode()



        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()))

            String currentLine

            while((currentLine =input.readLine())!=null){
                htmlData+=currentLine
                htmlData+="\n"
            }

        }else{
            println("Post did not work")
        }

        return htmlData


    }
}
