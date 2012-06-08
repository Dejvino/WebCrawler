package webcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 *  Web Reader
 * Trida pro zjednoduseni prace s webem - na zaklade URL vraci ziskany HTML kod
 * @author Dejvino
 */
public class WebReader {
    /**
     * Ziska zdrojovy kod stranky na dane URL
     * @param url
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static String get(String url) throws MalformedURLException, IOException {
        // vytvoreni spojeni
        URLConnection conn = new URL(url).openConnection();

        // pripojeni
        conn.connect();

        // ziskani vysledku
        StringBuilder sbIn = new StringBuilder();
        BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                            conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sbIn.append(inputLine);
        }
        in.close();

        // navraceni vysledku
        return sbIn.toString();
    }
}
