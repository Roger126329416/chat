import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private static final List<ClientInfo> clientWriters = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("client connected: " + socket.getInetAddress());
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        ClientInfo clientInfo = null;
        try (
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        ) {
            String clientName = bufferedReader.readLine();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Unknown";
            }

            clientInfo = new ClientInfo(clientName, bufferedWriter);
            clientWriters.add(clientInfo);

            String msgFromClient;
            while ((msgFromClient = bufferedReader.readLine()) != null) {
                System.out.println(clientName + ": " + msgFromClient);

                synchronized (clientWriters) {
                    for (ClientInfo info : clientWriters) {
                        if (info.writer != bufferedWriter) {
                            info.writer.write(clientName + ": " + msgFromClient);
                            info.writer.newLine();
                            info.writer.flush();
                        }
                    }
                }

                if ("bye".equalsIgnoreCase(msgFromClient)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected.");
        } finally {
            if (clientInfo != null) {
                synchronized (clientWriters) {
                    clientWriters.remove(clientInfo);
                }
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private static class ClientInfo {
        String name;
        BufferedWriter writer;
        ClientInfo(String name, BufferedWriter writer) {
            this.name = name;
            this.writer = writer;
        }


    }
}