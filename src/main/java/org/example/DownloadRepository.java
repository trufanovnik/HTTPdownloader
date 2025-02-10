package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class DownloadRepository {
    private static Semaphore semaphore = new Semaphore(1);

    protected static void download() {
        HttpClient client = HttpClient.newHttpClient();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/downloadLinks.txt"))) {
            String link;
            while ((link = reader.readLine()) != null) {
                String finalLink = link;
                new Thread(() -> downloadFile(client, finalLink)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void downloadFile(HttpClient client, String link){
        try{
            if (!semaphore.tryAcquire()){
                System.out.println("Steady: " + link);
                semaphore.acquire();
            }
            System.out.println("Downloading: " + link);
            String fileName = "file_" + link.hashCode() + ".zip";
            Path filePath = Paths.get("src/main/resources/downloaded_files", fileName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(link))
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(filePath));
            System.out.println("File " + fileName + " downloaded: ");

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }finally {
            semaphore.release();
        }
    }
}
