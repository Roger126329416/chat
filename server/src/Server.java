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
                if(msgFromClient.startsWith("/rename")){
                    String newName = msgFromClient.substring(8).trim();
                    if(!newName.isEmpty()){
                        String oldName = clientInfo.name;
                        clientInfo.name = newName;
                        clientName = newName;
                        System.out.println(oldName + " rename to: "+newName);

                        synchronized (clientWriters) {
                            for(ClientInfo info:clientWriters){
                                info.writer.write(oldName + "rename to: " + newName);
                                info.writer.newLine();
                                info.writer.flush();
                            }
                            
                        }
                        continue;
                    }
                }
                System.out.println(clientName + ": " + msgFromClient);

                if ("bye".equalsIgnoreCase(msgFromClient)) {
                    break;
                }

                synchronized (clientWriters) {
                    for (ClientInfo info : clientWriters) {
                        if (info.writer != bufferedWriter) {
                            info.writer.write(clientName + ": " + msgFromClient + "");
                            info.writer.newLine();
                            info.writer.flush();
                        }
                    }
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

    private void doCommand(String command){


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