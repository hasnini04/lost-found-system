package project_lostfound;

public class MapServerLauncher {
    public static void main(String[] args) {
        try {
            MapResultReceiver.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
