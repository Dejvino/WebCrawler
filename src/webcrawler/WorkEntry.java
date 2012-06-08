package webcrawler;

/**
 *  Pracovni zaznam
 * Zaznam s praci pro zpracovani vlaknem.
 * Obsahuje URL ke zpracovani a TTL (time to live) urcujici zbyvajici pocet
 * urovni zpracovani, nez bude vetev utnuta
 * @author Dejvino
 */
public class WorkEntry {

    private String url;

    private int ttl;

    /**
     * Vytvori tridu s danou URL a TTL
     * @param url URL ke zpracovani
     * @param ttl TimeToLive - pocet urovni, nez bude zpracovani ukonceno
     */
    public WorkEntry(String url, int ttl) {
        this.url = url;
        this.ttl = ttl;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
