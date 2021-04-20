package it.alex;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        Properties properties = new Properties();
        try {
            InputStream input = new FileInputStream("config/config.properties");
            properties.load(input);
        }catch (Exception e){
            e.printStackTrace();
        }
        String username = properties.get("username").toString();
        String password = properties.get("password").toString();
        String urlDirectory = properties.get("urlDirectory").toString();
        String delDirectory = properties.get("delDirectory").toString();
        String directoryInbox = properties.get("directoryInbox").toString();
        Integer sleepTime = Integer.parseInt(properties.get("sleepTime").toString());

        File dir = new File(directoryInbox);
        if (!dir.exists()){
            dir.mkdir();
        }

        while(true) {
            String login = username + ":" + password;
            String base64login = new String(Base64.encodeBase64(login.getBytes()));
            try {
                Document document = Jsoup
                        .connect(urlDirectory)
                        .header("Authorization", "Basic " + base64login)
                        .get();
                List<Element> list = document.select("td a").stream().filter(element -> element.toString().contains("xml")).collect(Collectors.toList());
                for (Element file : list) {
                    downloadSingle(directoryInbox, delDirectory, urlDirectory, username, password, file.attr("href"));
                }
                System.out.println("Sleeping : "+ sleepTime);
                Thread.sleep(sleepTime);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void downloadSingle(String directoryInbox, String delDirectory,String urlDirectory,String username, String password,String fileName){
        URL url;
        InputStream is = null;

        // Install Authenticator
        MyAuthenticator.setPasswordAuthentication(username, password);
        Authenticator.setDefault(new MyAuthenticator());

        try {
            url = new URL(urlDirectory+fileName);
            is = url.openStream();  // throws an IOException

            String fileNameReplaced = fileName.replaceAll("%23","#");

            File targetFile = new File(directoryInbox+"/"+fileNameReplaced);

            java.nio.file.Files.copy(
                    is,
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            IOUtils.closeQuietly(is);

            HttpClient client = new HttpClient();
            /* Implement an HTTP DELETE method on the resource */
            String  rep = delDirectory+fileName;/*fileName fileNameReplaced*/
            DeleteMethod delete = new DeleteMethod(rep);
            String login = username + ":" + password;
            String base64login = new String(Base64.encodeBase64(login.getBytes()));
            //delete.setRequestHeader("Authorization", token);
            delete.setRequestHeader("Authorization", "Basic " + base64login);
            //delete.setRequestHeader("password", password);
            /* Execute the HTTP DELETE request */
            client.executeMethod(delete);
            /* Display the response */
            System.out.println("Response status code: " + delete.getStatusCode());
            System.out.println("Response header: ");
            Header[] headers=delete.getResponseHeaders();

            for (int i = 0; i < headers.length; i++) {
                System.out.println(headers[i]);
            }


        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }

    }

}
