package test;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Handler extends Thread {

    private static final Map<String, String> CONTENT_TYPES = new HashMap() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");

        put("", "text/plain");
    }};

    private static final String NOT_FOUND_MESSAGE = "<h1>404 NOT FOUND<h1>";

    private Socket socket;

    private String directory;

    private StringBuffer sbuf = new StringBuffer();

    Handler(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (InputStream input = this.socket.getInputStream(); OutputStream output = this.socket.getOutputStream()) {
            Scanner reader = new Scanner(input).useDelimiter("\r\n");
            String line = "";
            sbuf.append("Input Request: \n");
            if(reader.hasNext()) {
                line = reader.next();
                sbuf.append(line).append("\n");
            }
            //System.out.println("First line : "+line);
            String []sts = line.split(" ");
            String method =sts[0];
            if(method.equals("GET")) {
                String url = sts[1];
                while (!line.equalsIgnoreCase(""))
                {
                    if(reader.hasNext()) {
                        line = reader.next();
                        sbuf.append(line).append("\n");
                    }
                }

                Path filePath = Paths.get(this.directory, url);
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    String type = Files.probeContentType(filePath);
                    System.out.println("type : "+type);
                    //byte[] fileBytes = Files.readAllBytes(filePath);
                    //output.write(fileBytes);
                    File file  = filePath.toFile();
                    long numToSend = file.length();
                    this.sendFileHeader(output, 200, "OK", type, numToSend);
                    long numSent = 0;
                    byte [] bytes = new byte[1];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    PrintStream ps = new PrintStream(output);
                    while(numSent < numToSend) {
                        long numThisTime = numToSend - numSent;
                        numThisTime = numThisTime < bytes.length ? numThisTime : bytes.length;
                        int numRead = fileInputStream.read(bytes, 0, (int) numThisTime);
                        if(numRead ==-1 ) break;
                        ps.write(bytes,0,numRead);
                        numSent += numRead;
                    }
                    ps.flush();
                    fileInputStream.close();
                    //output.flush();
                }
                else if (Files.exists(filePath) && Files.isDirectory(filePath)) {
                    System.out.println("present file path:" + filePath.toString());
                    List<Path> sub = Files.list(filePath).map(Path::getFileName).collect(Collectors.toList());
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("<html><body>");

                    try (Stream<Path> walk = Files.list(filePath)) {
                        List<String> result = walk.filter(Files::isDirectory).map(Path::toString).collect(Collectors.toList());
                        for (String name :
                                result) {
                            name = name.replaceFirst(this.directory, "");
                            stringBuilder.append("<a href=\"").append(name).append("\"><b>").append(name.substring(name.lastIndexOf('\\') + 1)).append("</b></a><br>");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try (Stream<Path> walk = Files.list(filePath)) {
                        List<String> result = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
                        for (String name :
                                result) {
                            name = name.replaceFirst(this.directory, "");
                            stringBuilder.append("<a href=\"").append(name).append("\">").append(name.substring(name.lastIndexOf('\\') + 1)).append("</a><br>");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    stringBuilder.append("</body></html>");
                    String type = CONTENT_TYPES.get("html");
                    byte[] fileBytes = stringBuilder.toString().getBytes();
                    this.sendHeader(output, 200, "OK", type, fileBytes.length);
                    output.write(fileBytes);
                }
                else {
                    System.out.println("not a file nor a directory: " + filePath);
                    String type = CONTENT_TYPES.get("text");
                    this.sendHeader(output, 404, "Not Found", type, NOT_FOUND_MESSAGE.length());
                    output.write(NOT_FOUND_MESSAGE.getBytes());
                    output.flush();
                }
            }

            else if (method.equals("UPLOAD")) {
                String fileName = sts[1].replaceAll("/", ".");
                if (fileName.equals("error.error")) {
                    System.out.println("Invalid File Name from Client");
                } else{
                    //System.out.println("File Name : "+fileName);
                    if (reader.hasNext()) {
                        line = reader.next();
                        sbuf.append(line).append("\n");
                    }
                sts = line.split(" ");
                while (!sts[0].equalsIgnoreCase("Content-Length:")) {
                    if (reader.hasNext()) {
                        line = reader.next();
                        sbuf.append(line).append("\n");
                        sts = line.split(" ");
                    }
                }
                int fileLength = Integer.parseInt(sts[1]);
                //System.out.println("File Length : "+fileLength);
                while (!line.equalsIgnoreCase("")) {
                    if (reader.hasNext()) {
                        line = reader.next();
                        sbuf.append(line).append("\n");
                    }
                }

                byte[] chunk = new byte[4 * 1024];

                int chunkLen = 0;
                File inputFile = new File(directory + File.separator + fileName);
                Files.deleteIfExists(inputFile.toPath());
                FileOutputStream fos = new FileOutputStream(inputFile, true);
                while ((chunkLen = input.read(chunk)) != -1) {
                    fos.write(chunk, 0, chunkLen);
                }
                fos.flush();
                fos.close();
                }
                String response ="HTTP/1.1 200 OK\r\n" +
                        "Server: Java HTTP Server: 1.0\r\n" +
                        "Connection: close\r\n" +
                        "Date: Sun Mar 15 00:36:14 BDT 2020\r\n\r\n";
                sbuf.append("Output Response - \n").append(response).append("\n");
            }
            FileOutputStream ffos = new FileOutputStream("log.txt",true);
            ffos.write(sbuf.toString().getBytes());
            ffos.flush();
            ffos.close();
            sbuf = new StringBuffer();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    private void sendFileHeader(OutputStream output, int statusCode, String statusText, String type, long lenght) {
        PrintStream ps = new PrintStream(output);
        sbuf.append("Output Response - \n");
        ps.printf("HTTP/1.1 %s %s\r\n", statusCode, statusText);
        ps.print("Server: Java HTTP Server: 1.0\r\n");
        ps.print("Connection: close\r\n");
        ps.printf("Date: %s\r\n",new Date());
        ps.printf("Content-Type: %s\r\n", "application/force-download");
        ps.printf("Content-Length: %s\r\n\r\n", lenght);
        //ps.printf("\r\n");
        ps.flush();

        sbuf.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\n");
        sbuf.append("Server: Java HTTP Server: 1.0\n");
        sbuf.append("Connection: close\n");
        sbuf.append("Date: ").append(new Date()).append("\n");
        sbuf.append("Content-Type: application/force-download\n" );
        sbuf.append("Content-Length: ").append(lenght).append("\n\n");
    }
    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long lenght) {
        PrintStream ps = new PrintStream(output);
        sbuf.append("Output Response - \n");
        ps.printf("HTTP/1.1 %s %s\r\n", statusCode, statusText);
        ps.printf("Server: Java HTTP Server: 1.0\r\n");
        ps.printf("Date: %s\r\n",new Date());
        ps.printf("Content-Type: %s\r\n", type);
        ps.printf("Content-Length: %s\r\n\r\n", lenght);
        //ps.printf("\r\n");
        ps.flush();

        sbuf.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\n");
        sbuf.append("Server: Java HTTP Server: 1.0\n");
        sbuf.append("Connection: close\n");
        sbuf.append("Date: ").append(new Date()).append("\n");
        sbuf.append("Content-Type: ").append(type).append("\n");
        sbuf.append("Content-Length: ").append(lenght).append("\n\n");
    }
}
