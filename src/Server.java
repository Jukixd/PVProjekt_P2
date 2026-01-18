import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class Server {
    private static final int PORT = 65525;
    private Bank core;
    private volatile boolean isRunning = true;
    private ServerSocket serverSocket;

    public Server(Bank core) {
        this.core = core;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server naslouchá na portu " + PORT);
            System.out.println("Napiš 'exit' pro vypnutí serveru.");
            Thread consoleListener = new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                while (isRunning) {
                    if (sc.hasNext()) {
                        String command = sc.next();
                        if (command.equalsIgnoreCase("exit")) {
                            stopServer();
                            break;
                        } else {
                            System.out.println("Neznámý příkaz. Pro konec napiš 'exit'.");
                        }
                    }
                }
            });
            consoleListener.start();
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHand worker = new ClientHand(clientSocket, core);
                    Thread t = new Thread(worker);
                    t.start();

                } catch (SocketException e) {
                    if (!isRunning) {
                        System.out.println("Server byl úspěšně vypnut.");
                        break;
                    } else {
                        System.err.println("Chyba socketu: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Chyba serveru: " + e.getMessage());
        }
    }
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Chyba při zavírání: " + e.getMessage());
        }
    }
}