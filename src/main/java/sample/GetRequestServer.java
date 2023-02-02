package sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GetRequestServer implements Runnable {
    private int port;
    private volatile String requestBody;

    public GetRequestServer(int port) {
        this.port = port;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    ArrayList<String> lines = new ArrayList<>();
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.isEmpty()) {
                            break;
                        }
                        lines.add(line);
                        //System.out.println(line);
                    }

                    this.requestBody = lines.get(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String getRequestBody() {
        return requestBody;
    }

    public synchronized void resetRequestBody() {
        this.requestBody = null;
    }
}

