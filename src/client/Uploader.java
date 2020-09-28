package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Uploader  {

    private static final String CRLF = "\r\n";

    private Socket socket;
    private String filename;

    Uploader(Socket socket, String fileName)
    {
        this.socket =  socket;
        this.filename = fileName;
    }


    public void run() {
        try (InputStream input = this.socket.getInputStream(); OutputStream output = this.socket.getOutputStream()) {
            PrintStream ps = new PrintStream(output);
            String methodType = "UPLOAD";
            String requestLine = methodType + " " + filename + " HTTP/1.1" + CRLF;
            Path filesPath = Paths.get(".", filename);
            if (!Files.isRegularFile(filesPath))
            {
                requestLine = methodType + " " + "error.error" + " HTTP/1.1" + CRLF;
                ps.print(requestLine);
                ps.flush();
                System.out.println("Invalid File name");
                return;
            }
            else {
            String contentType = Files.probeContentType(filesPath);
            File file = filesPath.toFile();
            String headerLines =
                    "Content-Length: " + file.length() + CRLF +
                    "Content-Type: " + contentType + CRLF +
                    "Date: " + new Date() + CRLF +
                    "Host: " + "localhost" + CRLF + CRLF;

            ps.print(requestLine);
            ps.print(headerLines);
            ps.flush();
                long numToSend = file.length();
                long numSent = 0;
                byte [] bytes = new byte[4*1024];
                FileInputStream fileInputStream = new FileInputStream(file);
                while(numSent < numToSend) {
                    long numThisTime = numToSend - numSent;
                    numThisTime = numThisTime < bytes.length ? numThisTime : bytes.length;
                    int numRead = fileInputStream.read(bytes, 0, (int) numThisTime);
                    if(numRead ==-1 ) break;
                    ps.write(bytes,0,numRead);
                    numSent += numRead;
                }
                fileInputStream.close();
                ps.flush();
                Thread.sleep(200);
                //byte  [] response = new byte[1024];
                //input.read(response);

            /*
            byte [] bb = Files.readAllBytes(filesPath);
            ps.write(bb);
            ps.flush();
            */

            }
        }catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
