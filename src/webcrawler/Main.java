package webcrawler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Vstupni bod aplikace
 *
 * @author Dejvino
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // pripraveni ulohy
        WorkEntry work = new WorkEntry("http://fi.muni.cz/", 6);

        // pripraveni instance zpracovani ulohy
        CrawlerWorker worker = new CrawlerWorker();

        worker.setConfSameDomain(true);

        // vytvoreni "poolu" threadu
        int teamSize = CrawlerThread.getTeamSize();
        CrawlerThread threads[] = new CrawlerThread[teamSize];
        for (int i = 0; i < teamSize; i++) {
            threads[i] = new CrawlerThread(worker);
        }

        // nastaveni prace prvnimu threadu
        threads[0].assignWork(work);


        // nastaveni propojeni kradeni prace
        for (int i = 1; i < teamSize; i++) {
            // i krade od 0
            threads[i].addVictim(threads[0]);
            // 0 krade od i (tedy od vsech)
            threads[0].addVictim(threads[i]);
        }

        // nastartovani
        for (int i = 0; i < teamSize; i++) {
           threads[i].start();
        }

        // pockani na ukonceni
        for (CrawlerThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                throw new InterruptedException("Cekani na vlakno selhalo: " + ex);
            }
        }

        // precteni vysledku
        System.out.println(worker.getResult());

        // vypis statistik
        for (int i = 0; i < teamSize; i++) {
            System.out.println("Vlakno " + i + " - " + threads[i].getStats());
        }
    }

}
