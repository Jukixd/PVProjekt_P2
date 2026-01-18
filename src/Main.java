public class Main {
    public static void main(String[] args) {
        Bank nd = new Bank();
        Server server = new Server(nd);
        server.start();
    }
}