import java.io.Serializable;

/**
 * Class representing a seq no used by the protocol in the packets.
 * Needs to be threadsafe.
 * @author satyajeet
 */
public class SeqNo implements Serializable {
    
    long s;
    
    SeqNo() {
        s = 1;
    }
    
    SeqNo(long s) {
        this.s = s;
    }
    
    public synchronized void incr() {
        s++;
    }
    
    public synchronized void set(long s) {
        this.s = s;
    }
    
    public synchronized long get() {
        return s;
    }
}
