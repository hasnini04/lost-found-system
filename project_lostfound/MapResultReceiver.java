package project_lostfound;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;

public class MapResultReceiver {
    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/save", (HttpExchange exchange) -> {
            String query = exchange.getRequestURI().getQuery();
            String[] parts = query != null ? query.split("&") : new String[0];
            String address = "";
            String type = "lost";

            for (String part : parts) {
                if (part.startsWith("address=")) {
                    address = URLDecoder.decode(part.substring("address=".length()), "UTF-8");
                } else if (part.startsWith("type=")) {
                    type = URLDecoder.decode(part.substring("type=".length()), "UTF-8");
                }
            }

            String filename = type.equals("found") ? "selected_location_found.txt" : "selected_location.txt";

            try (PrintWriter writer = new PrintWriter(filename)) {
                writer.println(address);
            }

            String response = "Saved";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.setExecutor(null); // Use default executor
        server.start();
        System.out.println("✅ MapResultReceiver listening at http://localhost:8080/save");
    }
}
