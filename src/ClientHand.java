import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHand implements Runnable {
    private Socket socket;
    private Bank core;

    public ClientHand(Socket socket, Bank core) {
        this.socket = socket;
        this.core = core;
    }

    @Override
    public void run() {
        String clientIp = socket.getInetAddress().getHostAddress();
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)
        ) {
            socket.setSoTimeout(30000);
            String request;
            while ((request = in.readLine()) != null) {
                try {
                    core.checkSpam(clientIp);
                } catch (IllegalStateException e) {
                    out.println("ER " + e.getMessage());
                    continue;
                }
                System.out.println("Příkaz od " + clientIp + ": " + request);
                String response = processCommand(request, request);
                out.println(response);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Klient " + clientIp + " vypršel (Timeout).");
        } catch (Exception e) {
            System.err.println("Chyba komunikace s " + clientIp + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
    private String processCommand(String request, String fullRequest) {
        String[] parts = request.split(" ");
        String cmd = parts[0].toUpperCase();

        try {
            switch (cmd) {
                case "BC": return "BC " + core.getMyIp();
                case "AC": return handleAccountCreate();
                case "BA": return "BA " + core.getTotalMoney();
                case "BN": return "BN " + core.getClientCount();
                case "AD": return handleDeposit(parts, fullRequest);
                case "AW": return handleWithdraw(parts, fullRequest);
                case "AB": return handleBalance(parts, fullRequest);
                case "AR": return handleRemove(parts);
                default: return "ER Neznámý příkaz";
            }
        } catch (Exception e) {
            return "ER Chyba zpracování: " + e.getMessage();
        }
    }
    private String handleAccountCreate() {
        try {
            String clientIp = socket.getInetAddress().getHostAddress();
            int newAcc = core.createAccount(clientIp);
            return "AC " + newAcc + "/" + core.getMyIp();
        } catch (IllegalStateException e) {
            return "ER " + e.getMessage();
        }
    }
    private String proxyRequest(String targetIp, String fullRequest) {
        System.out.println("--> Proxy: Volám banku " + targetIp);
        try (Socket s = new Socket(targetIp, 65525)) {
            s.setSoTimeout(5000);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            out.println(fullRequest);
            return in.readLine();
        } catch (Exception e) {
            return "ER Banka " + targetIp + " je nedostupná.";
        }
    }
    private String handleDeposit(String[] parts, String fullRequest) {
        if (parts.length != 3) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return proxyRequest(pa.ip, fullRequest);

        try {
            long amount = Long.parseLong(parts[2]);
            boolean success = core.updateBalance(pa.accNum, amount);
            return success ? "AD" : "ER Účet neexistuje";
        } catch (NumberFormatException e) {
            return "ER Částka musí být číslo";
        }
    }
    private String handleWithdraw(String[] parts, String fullRequest) {
        if (parts.length != 3) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return proxyRequest(pa.ip, fullRequest);
        try {
            long amount = Long.parseLong(parts[2]);

            boolean success = core.updateBalance(pa.accNum, -amount);
            return success ? "AW" : "ER Nedostatek peněz nebo neexistující účet";
        } catch (NumberFormatException e) {
            return "ER Částka musí být číslo";
        }
    }
    private String handleBalance(String[] parts, String fullRequest) {
        if (parts.length != 2) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return proxyRequest(pa.ip, fullRequest);

        long balance = core.getBalance(pa.accNum);
        return (balance == -1) ? "ER Účet neexistuje" : "AB " + balance;
    }

    private String handleRemove(String[] parts) {
        if (parts.length != 2) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return "ER Cizí banka";
        return core.removeAccount(pa.accNum) ? "AR" : "ER Nelze smazat";
    }

    private ParsedAccount parseAccount(String raw) {
        String[] s = raw.split("/");
        if (s.length != 2) return new ParsedAccount(0, "");
        return new ParsedAccount(Integer.parseInt(s[0]), s[1]);
    }

    private static class ParsedAccount {
        int accNum;
        String ip;
        ParsedAccount(int a, String i) { accNum = a; ip = i; }
        boolean isValidIp(String myIp) { return ip.equals(myIp); }
    }
}