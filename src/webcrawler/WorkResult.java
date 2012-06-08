package webcrawler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Vysledek zpracovane stranky
 * Obsahuje informace o zpracovane URL, seznam odkazu na strance pro dalsi
 * zpracovani, statistiky HTML tagu (pozadovana vysledna data).
 * @author Dejvino
 */
public class WorkResult {

    /**
     * Zpracovana URL
     */
    private String url;

    /**
     * Seznam odkazu na strance
     */
    private Set<String> links;

    /**
     * Tabulka tag -> pocet vyskytu
     */
    private Map<String, Integer> stats;

    /**
     * Vytvori zaznam o vysledku prace s danou URL, seznamem odkazu a statistikou.
     * Data se klonuji.
     * @param url
     * @param links
     * @param stats
     */
    public WorkResult(String url, Set<String> links, Map<String, Integer> stats) {
        this.setUrl(url);
        this.setLinks(links);
        this.setStats(stats);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<String> getLinks() {
        return Collections.unmodifiableSet(links);
    }

    public void setLinks(Set<String> links) {
        this.links = new HashSet<String>(links);
    }

    public Map<String, Integer> getStats() {
        return Collections.unmodifiableMap(stats);
    }

    public void setStats(Map<String, Integer> stats) {
        this.stats = new HashMap<String, Integer>(stats);
    }
}
