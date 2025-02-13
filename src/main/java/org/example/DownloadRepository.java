package org.example;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

public class DownloadRepository {
    private static Semaphore semaphore = new Semaphore(2);
    private static java.nio.file.Files Files;

    protected static void download() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

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

            String fileName = getFileName(link);
            Path filePath = getSavingPath(fileName);

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

    private static Path getSavingPath(String fileName) throws IOException {
        Path downloadDir = Paths.get("src/main/resources/downloaded_files");
        if (!Files.exists(downloadDir)) {
            Files.createDirectories(downloadDir);
        }
        Path path = Paths.get("src/main/resources/downloaded_files", fileName);
        return path;
    }

    private static String getFileName(String link) {
        String[] parts = link.split("/");
        String owner = parts[parts.length - 4]; // Владелец репозитория
        String repo = parts[parts.length - 3];   // Имя репозитория
        String branch = parts[parts.length - 1];

        String fileName = owner + "_" + repo + "_" + branch + ".zip";
        return fileName;
    }
}
