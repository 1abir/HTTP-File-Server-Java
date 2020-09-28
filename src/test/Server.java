package test;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;

    private String directory;

    public Server(int port, String directory) {
        this.port = port;
        this.directory = directory;
    }

    void start() {
        try (ServerSocket server = new ServerSocket(this.port)) {
            while (true) {
                Socket socket = server.accept();
                Handler thread = new Handler(socket, this.directory);
                thread.start();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt("8799");
        String directory = "root" ;
        new Server(port, directory).start();
    }
}
