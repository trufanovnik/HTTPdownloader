package org.example;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GetDownloadLinkFromGithub {
    static String OWNER;
    static String REPONAME;
    static String DEFAULT_BRANCH;

    protected static void getAndSaveDownloadLinks() throws IOException {
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader("src/main/resources/repositories.txt"));
            writer = new BufferedWriter(new FileWriter("src/main/resources/downloadLinks.txt", false));

            String line;
            while ((line = reader.readLine()) != null) {
                String link = buildLink(line);
                saveLink(writer, link);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    protected static String buildLink(String repoLink) throws IOException, InterruptedException {
        String[] parts = repoLink
                .replace("https://github.com/", "")
                .split("/");
        OWNER = parts[0];
        REPONAME = parts[1];

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://api.github.com/repos/%s/%s",OWNER, REPONAME)))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200){
            String responseBody = response.body();
            DEFAULT_BRANCH = responseBody
                    .split("\"default_branch\":\"")[1]
                    .split("\"")[0];
        }else {
            throw new IOException("Failed to get repository info. Status code: " + response.statusCode());
        }
        return String.format("https://api.github.com/repos/%s/%s/zipball/%s", OWNER, REPONAME, DEFAULT_BRANCH);
    }

    protected static void saveLink(BufferedWriter writer, String link) throws IOException {
        writer.write(link);
        writer.newLine();
    }
}
