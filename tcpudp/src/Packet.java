import java.io.Serializable;

/**
 * Class representing the packet 
 * @author Sushrut
 */
public class Packet implements Serializable{
    
    byte[] data;
    boolean cmdPacket;
    String sourceIp, destIp;
    int sourcePort, destPort;
    SeqNo seqNo;

    /**
     * Constructor creating the packet
     * @param data
     * @param cmdPacket
     * @param sourceIp
     * @param sourcePort
     * @param destIp
     * @param destPort
     * @param seqNo
     */
    Packet(byte[] data, boolean cmdPacket, String sourceIp, int sourcePort, 
            String destIp, int destPort, SeqNo seqNo) {
        this.data = data;
        this.cmdPacket = cmdPacket;    
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.destIp = destIp;
        this.destPort = destPort;
        this.seqNo = seqNo;
    }
}