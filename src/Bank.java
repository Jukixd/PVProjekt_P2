import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Bank {
    private String myIp;
    private ConcurrentHashMap<Integer, Long> accounts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> ipAccLimits = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Deque<Long>> spamHistory = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> bannedIps = new ConcurrentHashMap<>();

    public Bank() {
        setupIP();
        loadDataFromDB();
    }

    private void setupIP() {
        try {
            this.myIp = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) { this.myIp = "127.0.0.1"; }
    }

    private void loadDataFromDB() {
        String sql = "SELECT acc_num, balance, owner_ip FROM accounts";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            accounts.clear();
            ipAccLimits.clear();

            while (rs.next()) {
                int acc = rs.getInt("acc_num");
                long bal = rs.getLong("balance");
                String ip = rs.getString("owner_ip");

                accounts.put(acc, bal);
                if (ip != null && !ip.isEmpty()) {
                    ipAccLimits.put(ip, ipAccLimits.getOrDefault(ip, 0) + 1);
                }
            }
            System.out.println("--- DB NAČTENA: " + accounts.size() + " účtů ---");
        } catch (SQLException e) {
            System.err.println("Kritická chyba při načítání z MySQL: " + e.getMessage());
        }
    }

    public synchronized int createAccount(String clientIp) {
        int count = ipAccLimits.getOrDefault(clientIp, 0);
        if (count >= 3) throw new IllegalStateException("Limit 3 účty na IP překročen.");

        int newAcc = 10000 + new Random().nextInt(90000);
        while (accounts.containsKey(newAcc)) newAcc = 10000 + new Random().nextInt(90000);

        String sql = "INSERT INTO accounts (acc_num, balance, owner_ip) VALUES (?, 0, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newAcc);
            pstmt.setString(2, clientIp);
            pstmt.executeUpdate();
            accounts.put(newAcc, 0L);
            ipAccLimits.put(clientIp, count + 1);
            return newAcc;
        } catch (SQLException e) {
            throw new RuntimeException("Chyba při zápisu do MySQL: " + e.getMessage());
        }
    }

    public synchronized boolean updateBalance(int accNum, long amountChange) {
        if (!accounts.containsKey(accNum)) return false;

        long currentBalance = accounts.get(accNum);
        long newBalance = currentBalance + amountChange;

        if (newBalance < 0) return false;

        String sql = "UPDATE accounts SET balance = ? WHERE acc_num = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, newBalance);
            pstmt.setInt(2, accNum);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                accounts.put(accNum, newBalance);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("DB Update Error: " + e.getMessage());
        }
        return false;
    }

    public synchronized boolean removeAccount(int accNum) {
        if (!accounts.containsKey(accNum) || accounts.get(accNum) > 0) return false;

        String sql = "DELETE FROM accounts WHERE acc_num = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accNum);
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                accounts.remove(accNum);
                return true;
            }
        } catch (SQLException e) {

        }
        return false;
    }

    public String getMyIp() { return myIp; }
    public long getBalance(int acc) { return accounts.getOrDefault(acc, -1L); }
    public int getClientCount() { return accounts.size(); }
    public long getTotalMoney() { return accounts.values().stream().mapToLong(Long::longValue).sum(); }

    public synchronized void checkSpam(String ip) {
        long now = System.currentTimeMillis();
        if (bannedIps.containsKey(ip) && now < bannedIps.get(ip)) {
            throw new IllegalStateException("Máš BAN za spam. Zkus to později.");
        }
        spamHistory.putIfAbsent(ip, new ArrayDeque<>());
        Deque<Long> times = spamHistory.get(ip);
        times.addLast(now);
        while (!times.isEmpty() && now - times.peekFirst() > 1000) times.removeFirst();
        if (times.size() > 2) {
            bannedIps.put(ip, now + 30000);
            throw new IllegalStateException("STOP SPAM! Ban 30s.");
        }
    }
}