package webcrawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Dejvino
 */
public class CrawlerWorker {

    private ConcurrentHashMap<String, Future<WorkResult>> cache
            = new ConcurrentHashMap<String, Future<WorkResult>>();

    /**
     * Ma se prochazet pouze stejna domena?
     */
    private boolean confSameDomain = false;

    public boolean isConfSameDomain() {
        return confSameDomain;
    }

    public void setConfSameDomain(boolean confSameDomain) {
        this.confSameDomain = confSameDomain;
    }

    /**
     * Zpracuje danou URL a vrati vysledne informace.
     * @param url URL ke zpracovani
     * @return Ziskane informace
     * @throws InterruptedException
     */
    public WorkResult process(final String url) throws InterruptedException, ExecutionException
    {
        final CrawlerWorker worker = this;
        while (true) {
            // ziskani vysledku z cache
            Future<WorkResult> f = cache.get(url);
            // cache je prazdna
            if (f == null) {
                // jeste neprobehlo zpracovani
                // priprava ulohy ke spusteni
                Callable<WorkResult> eval = new Callable<WorkResult>() {
                    public WorkResult call() throws InterruptedException {
                        return worker.crawl(url);
                    }
                };
                // priprava ulohy ke spusteni
                FutureTask<WorkResult> ft = new FutureTask<WorkResult>(eval);
                // pridani na seznam vysledku
                f = cache.putIfAbsent(url, ft);
                // pokud jde o prvni pridani, spust ulohu
                if (f == null) { f = ft; ft.run(); }
            }
            // pokus ziskat vysledek
            try {
                return f.get();
            } catch (CancellationException e) {
                // uloha byla zrusena, je treba ji odebrat ze seznamu
                cache.remove(url, f);
            }
        }
    }

    protected WorkResult crawl(final String url) {
        // pripraveni kolekci pro data
        HashSet<String> links = new HashSet<String>();
        HashMap<String, Integer> stats = new HashMap<String, Integer>();

        try {
            // nacteni stranky
            String source = WebReader.get(url);

            // priprava domeny odkazu
            String domain = "";
            Pattern domainPattern = Pattern.compile("^http[s]?://([\\-a-zA-Z.0-9]+)/");
            Matcher domainMatcher = domainPattern.matcher(url);
            if (domainMatcher.find() && domainMatcher.groupCount() == 1) {
                domain = domainMatcher.group(1);
            }

            // zpracovani stranky
            // priprava regularniho vyrazu
            Pattern pattern = Pattern.compile("href=\"([^\"]*)\"");
            // vytvoreni vyhledavace
            Matcher matcher = pattern.matcher(source);
            // vyhledavani
            while (matcher.find() == true) {
                // kontrola vysledku
                if (matcher.groupCount() != 1) {
                    // neplatny pocet skupin
                    continue;
                }
                String link = matcher.group(1);

                // kontrola odkazu
                if (!(link.matches(".*\\.php$") || link.matches(".*\\.phtml$")
                        || link.matches(".*\\.html$") || link.matches(".*\\.htm$")
                        || link.matches(".*/$")
                        || link.matches(".*\\.asp$") || link.matches(".*\\.jsp$"))) {
                    //continue;
                }

                // uprava odkazu na absolutni
                if (link.startsWith("/")) {
                    link = "http://" + domain + link;
                }

                // kontrola zmeny domeny
                if (this.confSameDomain && link.startsWith("http") && !link.contains(domain)) {
                    continue;
                }

                // pridani do seznamu
                links.add(link);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(CrawlerWorker.class.getName()).log(Level.FINE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CrawlerWorker.class.getName()).log(Level.SEVERE, null, ex);
        }

        // vraceni vysledku
        WorkResult result = new WorkResult(url, links, stats);
        return result;
    }

    public String getResult() {
        return "...";
    }
}
