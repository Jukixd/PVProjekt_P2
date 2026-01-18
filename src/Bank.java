import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Bank {
    private static final String DATA_FILE = "bank_data.txt";
    private ConcurrentHashMap<Integer, Long> accounts = new ConcurrentHashMap<>();
    private String myIp;

    public Bank() {
        try {
            this.myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            this.myIp = "127.0.0.1";
        }
        System.out.println("--- STARTUJI BANKU: " + myIp + " ---");
        loadData();
    }

    public String getMyIp() {
        return myIp;
    }
    public synchronized int createAccount() {
        Random rand = new Random();
        int newAcc;
        do {
            newAcc = 10000 + rand.nextInt(90000);
        } while (accounts.containsKey(newAcc));
        accounts.put(newAcc, 0L);
        saveData();
        return newAcc;
    }
    public long getBalance(int accNum) {
        return accounts.getOrDefault(accNum, -1L);
    }
    public synchronized boolean deposit(int accNum, long amount) {
        if (!accounts.containsKey(accNum)) return false;
        if (amount < 0) return false;

        accounts.put(accNum, accounts.get(accNum) + amount);
        saveData();
        return true;
    }
    public synchronized boolean withdraw(int accNum, long amount) {
        if (!accounts.containsKey(accNum)) return false;
        long current = accounts.get(accNum);
        if (current < amount) return false;

        accounts.put(accNum, current - amount);
        saveData();
        return true;
    }
    public synchronized boolean removeAccount(int accNum) {
        if (!accounts.containsKey(accNum)) return false;
        if (accounts.get(accNum) > 0) return false; // Jsou tam prachy

        accounts.remove(accNum);
        saveData();
        return true;
    }

    public long getTotalMoney() {
        long total = 0;
        for (long val : accounts.values()) total += val;
        return total;
    }

    public int getClientCount() {
        return accounts.size();
    }
    private void saveData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Map.Entry<Integer, Long> entry : accounts.entrySet()) {
                writer.println(entry.getKey() + ";" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Chyba ukládání: " + e.getMessage());
        }
    }

    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    accounts.put(Integer.parseInt(parts[0]), Long.parseLong(parts[1]));
                }
            }
            System.out.println("Načteno účtů: " + accounts.size());
        } catch (Exception e) {
            System.err.println("Chyba načítání.");
        }
    }
}