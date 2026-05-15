package server;

public class ParkServerMain {

    public static void main(String[] args) {
        int port = 5555;

        ParkServer server = new ParkServer(port);

        try {
            server.listen();
            System.out.println("Park Server running on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}