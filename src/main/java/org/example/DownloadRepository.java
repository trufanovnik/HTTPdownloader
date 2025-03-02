package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadRepository {
    private static final long RATE_LIMIT_BYTES_PER_SECOND = 900 * 1024;
    private static final AtomicLong BYTES_WRITTEN = new AtomicLong(0);
    private static final String PATH_LINKS_FOR_DOWNLOAD = "src/main/resources/downloadLinks.txt";
    private static final String DOWNLOAD_DIRECTORY =  "src/main/resources/downloaded_files";
    private static final int BUFFER_SIZE = 8192;

    private static long startTime = System.currentTimeMillis();
    private static Semaphore semaphore = new Semaphore(2);
    private static Files Files;

    protected static void download() {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        try (BufferedReader reader = new BufferedReader(new FileReader(PATH_LINKS_FOR_DOWNLOAD))) {
            String link;
            while ((link = reader.readLine()) != null) {
                String finalLink = link;
                new Thread(() -> downloadFile(client, finalLink)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void downloadFile(HttpClient client, String link){
        try {
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

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream inputStream = response.body();
                 OutputStream outputStream = Files.newOutputStream(filePath)) {

                byte[] buffer = new byte[BUFFER_SIZE]; // Буфер для чтения данных
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // Записываем данные в файл
                    outputStream.write(buffer, 0, bytesRead);

                    // Увеличиваем счётчик записанных байт
                    BYTES_WRITTEN.addAndGet(bytesRead);

                    // Проверяем скорость и при необходимости добавляем задержку
                    throttle();
                }
            }
            System.out.println("File " + fileName + " downloaded: ");
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }finally {
            semaphore.release();
        }
    }
    private static void throttle() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        // Рассчитываем, сколько байт можно записать за текущее время
        long allowedBytes = (elapsedTime * RATE_LIMIT_BYTES_PER_SECOND) / 1000;

        // Если записано больше, чем разрешено, добавляем задержку
        if (BYTES_WRITTEN.get() > allowedBytes) {
            long sleepTime = (BYTES_WRITTEN.get() * 1000 / RATE_LIMIT_BYTES_PER_SECOND) - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Download interrupted", e);
                }
            }
        }
    }

    private static Path getSavingPath(String fileName) throws IOException {
        Path downloadDir = Paths.get(DOWNLOAD_DIRECTORY);
        if (!Files.exists(downloadDir)) {
            Files.createDirectories(downloadDir);
        }
        Path path = Paths.get(DOWNLOAD_DIRECTORY, fileName);
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
