import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Class having functionality to download file from server and send cmd
 * @author Sushrut
 */
public class Client extends Thread {

    final InetAddress localhost;
    final int serverPort;
    final int nackServerPort;
    static String serverIp;
    String ip;//ip of self

    //cmd or file
    String userInput;
    
    Client(String userInput) throws UnknownHostException {        
        this.userInput = userInput;
        localhost = InetAddress.getByName("127.0.0.1");
        serverPort = 5000;
        nackServerPort = 5005;
        ip = InetAddress.getLocalHost().toString();
        String[] temp = ip.split("/");
        ip = temp[1];        
    }
    
    //ask and received file or sends cmd packet in a thread
    public void run() {
        
        Packet p, x;
        FileOutputStream fos = null;
        DatagramSocket socket;
        DatagramPacket udpPacket;
        ByteArrayOutputStream out;
        ObjectOutputStream os;
        ByteArrayInputStream bis;
        ObjectInputStream in;
        byte[] packetInBytes;
        boolean timedOut = false;
        
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            
            if ( userInput.equals("cmd") ) {
                //just send the cmd packet
                p = new Packet(null, true, ip, socket.getLocalPort(), serverIp, serverPort, new SeqNo());

                //convert Packet object to bytes
                out = new ByteArrayOutputStream();
                os = new ObjectOutputStream(out);
                os.writeObject(p);
                packetInBytes = out.toByteArray();
                os.close();
                out.close();

                //put it in datagrampacket to send to server
                udpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, InetAddress.getByName(serverIp), serverPort);
                socket.send(udpPacket);
            } else if ( userInput.equals("file")) {
                fos = new FileOutputStream(new File("out.txt"));

                //send file request packet
                p = new Packet(null, false, ip, socket.getLocalPort(), serverIp, serverPort, new SeqNo());

                //convert Packet object to bytes
                out = new ByteArrayOutputStream();
                os = new ObjectOutputStream(out);
                os.writeObject(p);
                packetInBytes = out.toByteArray();
                os.close();
                out.close();

                //put it in datagrampacket to send to server
                udpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, InetAddress.getByName(serverIp), serverPort);
                socket.send(udpPacket);
                
                SeqNo recvdSeqNo = new SeqNo(0);
                //now receive file in chunks
                while ( true ) {
                    try {
                        packetInBytes = new byte[1500];
                        udpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, localhost, serverPort);
                        socket.receive(udpPacket);                    
                        bis = new ByteArrayInputStream(packetInBytes);
                        in = new ObjectInputStream(bis);                                        
    
                        x = (Packet) in.readObject();
    
                        if ( (recvdSeqNo.get() + 1) == x.seqNo.get() ) {
                            recvdSeqNo.incr();
                        } else {
                            //send NACK
                            Packet nackPacket = new Packet(null, false, "127.0.0.1", socket.getLocalPort(), serverIp, nackServerPort, recvdSeqNo);
                            out = new ByteArrayOutputStream();
                            os = new ObjectOutputStream(out);
                            os.writeObject(nackPacket);
                            packetInBytes = new byte[1500];
                            packetInBytes = out.toByteArray();
                            os.close();
                            out.close();
                            DatagramPacket nackUdpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, InetAddress.getByName(serverIp), nackServerPort);
                            socket.send(nackUdpPacket);
                            continue;
                        }
                        
                        //if last packet then break
                        if ( x.data == null )
                            break;

                        fos.write(x.data);
                        System.out.println("Received: " + x.seqNo.get());
                    } catch (Exception e) {
                        Packet nackPacket = new Packet(null, false, "127.0.0.1", socket.getLocalPort(), serverIp, nackServerPort, recvdSeqNo);
                        out = new ByteArrayOutputStream();
                        os = new ObjectOutputStream(out);
                        os.writeObject(nackPacket);
                        packetInBytes = new byte[1500];
                        packetInBytes = out.toByteArray();
                        os.close();
                        out.close();
                        DatagramPacket nackUdpPacket = new DatagramPacket(packetInBytes, packetInBytes.length, InetAddress.getByName(serverIp), nackServerPort);
                        socket.send(nackUdpPacket);
                    }
                }
                fos.close();
            } else {
                System.out.println("weird error");
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    /**
     * Main method taking input from cmd line and making request accordingly
     * @param args
     */
    public static void main(String args[]) {
        if ( args.length != 1 ) {
            System.out.println("Enter IP of server");
            return;
        }
             
        Client.serverIp = args[0];
        String userInput;
        Scanner sc = new Scanner(System.in);
        while ( true ) {
            try {
                userInput = sc.nextLine();             
                if ( userInput.equals("cmd") || userInput.equals("file") ) {
                    (new Client(userInput)).start();                    
                } else {
                    System.out.println("Enter cmd or file");
                    continue;
                }                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}