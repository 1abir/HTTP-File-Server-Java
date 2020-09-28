package client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private String host;

    private int port;
    private String fileName;

    public Client(String host, int port, String fileName) {
        this.host = host;
        this.port = port;
        this.fileName = fileName;
    }
    @Override
    public void run() {
        try (Socket socket = new Socket(this.host,this.port)) {
                Uploader uploader = new Uploader(socket,fileName);
                uploader.run();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt("8799");
        while (true) {
            System.out.println("Enter File Name to Upload : ");
            Scanner scanner =new Scanner(System.in);
            String fileName =  scanner.nextLine();
            if(fileName.equalsIgnoreCase("exit"))
                break;
            Client client = new Client("localhost",port,fileName);
            client.start();
        }
    }
}
