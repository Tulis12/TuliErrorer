package dev.tulis.errorer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Errorer {
    String repoPath;
    String token;
    int labelId = 1;

    public Errorer(String repoPath, String token) {
        this.repoPath = repoPath;
        this.token = token;
    }

    public Errorer(String repoPath, String token, int labelId) {
        this.repoPath = repoPath;
        this.token = token;
        this.labelId = labelId;
    }

    public void reportException(Exception e, Object o, String... additionalData) {
        String stackTrace = getErrorStackTrace(e);
        String className = o.getClass().getName();

        ZonedDateTime warsawTime = ZonedDateTime.now(ZoneId.of("Europe/Warsaw"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedTime = warsawTime.format(formatter);

        StringBuilder description = new StringBuilder("### Error stackTrace: \n```\n");
        description.append(stackTrace);
        description.append("```\n\n");
        for(String str : additionalData) {
            description.append(str);
            if(!additionalData[additionalData.length - 1].equals(str)) description.append(" ");
        }
        if(additionalData.length != 0) description.append("\n");
        description.append("Automatically reported by TuliErrorer");

        try(HttpClient client = HttpClient.newHttpClient()) {
            String json = String.format("""
            {
                "title": "%s",
                "body": "%s",
                "assignees": ["Tulis"],
                "labels": [1]
            }
            """, e.getMessage() + " in " + className + " at " + formattedTime, description);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://git.tulisiowice.top/api/v1/repos/" + repoPath + "/issues"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "token " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getErrorStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getLabelId() {
        return labelId;
    }

    public void setLabelId(int labelId) {
        this.labelId = labelId;
    }
}