package webcrawler;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dejvino
 */
public class CrawlerThread extends Thread {

    /**
     * Pocet vlaken bez prace - slouzi jako synchronizacni bariera pro ukonceni
     */
    private static final AtomicInteger thievesCount = new AtomicInteger(0);

    /**
     * Oboustranna fronta pro praci
     */
    private Deque<WorkEntry> workDeque = new ArrayDeque<WorkEntry>();

    /**
     * Instance provadejici praci
     */
    private CrawlerWorker worker;

    /**
     * Vlakno, od ktereho toto vlakno krade praci
     */
    private List<CrawlerThread> victims;

    private int statsLocalDeque;
    private int statsRemoteDeque;

    /**
     * Konstruktor vlakna, ve kterem je prirazena konkretni uloha, kterou vlakno provadi
     * @param worker
     */
    public CrawlerThread(CrawlerWorker worker) {
        if (worker == null) {
            throw new NullPointerException("worker");
        }
        this.worker = worker;

        this.victims = new LinkedList<CrawlerThread>();
        
        this.statsLocalDeque = 0;
        this.statsRemoteDeque = 0;
    }

    @Override
    /**
     * Hlavni funkce vlakna
     * Provadi rezii kolem zpracovani ukolu - kontroluje podminku ukonceni
     * behu, predava praci ke zpracovani, pripadne pokud prace neni, tak se ji
     * pokusi "ukrast" cizimu vlaknu.
     */
    public void run() {
        // priznak, ze toto vlakno se snazi ukrast praci
        boolean isThief = false;
        // cislo posledni obeti kradeni prace
        int victimNumber = 0;
        // pokud vsechna vlakna zkouseji ukrast praci ostatnim,
        // vypocet skoncil (jakmile vzroste thievesCount na velikost teamu,
        // uz nikdy neklesne - vsichni jen kradou a nikdo nepracuje)
        while(thievesCount.get() != CrawlerThread.getTeamSize()) {
            if (!isThief) {
                try {
                    // zpracovani lokalni fronty
                    this.process(this.popWork());
                    this.statsLocalDeque++;
                } catch (NoSuchElementException ex) {
                    // prazdna fronta, stavame se zlodejem
                    isThief = true;
                    thievesCount.incrementAndGet();
                } catch (InterruptedException ex) {
                    Logger.getLogger(CrawlerThread.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                } catch (ExecutionException ex) {
                    Logger.getLogger(CrawlerThread.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            } else if(isThief && this.victims.size() > 0) {
                try {
                    // ukradeni cizi prace
                    victimNumber = (victimNumber + 1) % this.victims.size();
                    WorkEntry work = this.victims.get(victimNumber).stealWork();
                    if (work == null) {
                        throw new NoSuchElementException();
                    }
                    // ukradeni se povedlo, jiz nejsme zlodejem
                    isThief = false;
                    thievesCount.decrementAndGet();
                    //this.history += " S";
                    // zpracovani nove prace
                    this.process(work);
                    this.statsRemoteDeque++;
                } catch (NoSuchElementException ex) {
                    // prazdna fronta / nepovedlo se kradeni
                } catch (InterruptedException ex) {
                    Logger.getLogger(CrawlerThread.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                } catch (ExecutionException ex) {
                    Logger.getLogger(CrawlerThread.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
        }
    }

    /**
     * Interni metoda pro zpracovani prace - necha zpracovat URL prirazenou
     * ulohou, ziskane URL si prida do fronty
     * @param work Prace ke zpracovani
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void process(WorkEntry work) throws InterruptedException, ExecutionException {
        // zpracovani prace
        WorkResult result = this.worker.process(work.getUrl());
        //System.out.println(work.getUrl());
        // pokud jeste je kam pokracovat
        if (work.getTtl() > 0) {
            // ulozeni ziskanych linku jako nove prace
            for (String url : result.getLinks()) {
                this.pushWork(new WorkEntry(url, work.getTtl() - 1));
            }
        }
        //this.history += " " + this.workDeque.size();
    }

    /**
     * Nastavi praci, kterou bude dale vlakno zpracovavat (a pripadne vymaze
     * predchozi prirazenou praci).
     * Slouzi jako inicializacni metoda.
     * @param work Prace ke zpracovani
     */
    public void assignWork(WorkEntry work) {
        this.workDeque.clear();
        this.workDeque.push(work);
    }

    /**
     * Nastavi obet, od ktere bude toto vlakno krast praci
     * @param victim Obet
     */
    public void addVictim(CrawlerThread victim) {
        this.victims.add(victim);
    }

    /**
     * Odebere z lokalni deque prace jeden zaznam.
     * Volani je neblokujici, pokud je fronta prazdna, je vysledkem null.
     * @return Dalsi zaznam ke zpracovani nebo null
     */
    private synchronized WorkEntry popWork() throws NoSuchElementException {
        return this.workDeque.pop();
    }

    /**
     * Prida jeden zaznam s praci do lokalni deque
     * @param work Zaznam k pridani
     */
    private synchronized void pushWork(WorkEntry work) {
        this.workDeque.push(work);
    }

    /**
     * Prida kolekci s praci do lokalni deque
     * @param work Kolekce zaznamu k pridani
     */
    private void pushMoreWork(Collection<WorkEntry> work) {
        for (WorkEntry entry : work) {
            this.workDeque.push(entry);
        }
    }
    
    /**
     * Odebere (ukradne) z lokalni deque prace zaznam z vrchu fronty.
     * Volani je neblokujici, pokud je fronta prazdna nebo se nepodarilo
     * praci ziskat, je vysledkem null.
     * @return Ukradeny zaznam ke zpracovani nebo null
     */
    protected synchronized WorkEntry stealWork() {
        // prenechani kontextu
        Thread.yield();
        // pokus ukrast praci
        return this.workDeque.pollLast();
    }

    /**
     * Vraci pocet vlaken v "teamu", ktere zpracovavaji ulohy
     * @return Velikost teamu vlaken
     */
    protected static int getTeamSize() {
        return Runtime.getRuntime().availableProcessors() + 1;
    }

    public String getStats() {
        return String.format("Local: %8d  Remote: %8d  Total: %8d",
                this.statsLocalDeque, this.statsRemoteDeque,
                (this.statsLocalDeque + this.statsRemoteDeque));
    }
}
