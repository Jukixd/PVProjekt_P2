import java.io.*;
import java.net.Socket;

public class ClientHand implements Runnable {
    private Socket socket;
    private Bank core;

    public ClientHand(Socket socket, Bank core) {
        this.socket = socket;
        this.core = core;
    }
    @Override
    public void run() {
        try {
            socket.setSoTimeout(60000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            String request = in.readLine();
            if (request != null) {
                System.out.println("Příkaz od " + socket.getInetAddress() + ": " + request);
                String response = processCommand(request);
                out.println(response);
            }
            socket.close();
        } catch (Exception e) {
            System.err.println("Chyba vlákna: " + e.getMessage());
        }
    }
    private String processCommand(String request) {
        String[] parts = request.split(" ");
        String cmd = parts[0].toUpperCase();

        try {
            switch (cmd) {
                case "BC": return "BC " + core.getMyIp();
                case "AC": return "AC " + core.createAccount() + "/" + core.getMyIp();
                case "BA": return "BA " + core.getTotalMoney();
                case "BN": return "BN " + core.getClientCount();
                case "AD": return handleDeposit(parts);
                case "AW": return handleWithdraw(parts);
                case "AB": return handleBalance(parts);
                case "AR": return handleRemove(parts);
                default: return "ER Neznámý příkaz";
            }
        } catch (Exception e) {
            return "ER Chyba zpracování: " + e.getMessage();
        }
    }
    private String handleDeposit(String[] parts) {
        if (parts.length != 3) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return "ER Cizí banka (Proxy zatím neumím)";

        long amount = Long.parseLong(parts[2]);
        boolean success = core.deposit(pa.accNum, amount);
        return success ? "AD" : "ER Účet neexistuje nebo špatná částka";
    }
    private String handleWithdraw(String[] parts) {
        if (parts.length != 3) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return "ER Cizí banka";

        long amount = Long.parseLong(parts[2]);
        boolean success = core.withdraw(pa.accNum, amount);
        return success ? "AW" : "ER Nedostatek peněz nebo neexistující účet";
    }
    private String handleBalance(String[] parts) {
        if (parts.length != 2) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return "ER Cizí banka";

        long balance = core.getBalance(pa.accNum);
        return (balance == -1) ? "ER Účet neexistuje" : "AB " + balance;
    }
    private String handleRemove(String[] parts) {
        if (parts.length != 2) return "ER Chybný formát";
        ParsedAccount pa = parseAccount(parts[1]);
        if (!pa.isValidIp(core.getMyIp())) return "ER Cizí banka";

        boolean success = core.removeAccount(pa.accNum);
        return success ? "AR" : "ER Účet má peníze nebo neexistuje";
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