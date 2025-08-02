package dev.tulis.errorer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Errorer {
    String repoPath;
    String token;
    int labelId = 1;
    int duplicateId = 2;
    String assignee = "Tulis";

    public Errorer(String repoPath, String token) {
        this.repoPath = repoPath;
        this.token = token;
    }

    public void reportException(Exception e, Object o, String... additionalData) {
        String stackTrace = getErrorStackTrace(e);
        String className = o.getClass().getName();
        String errorText = e.getClass().getName() + " in " + className;

        ZonedDateTime warsawTime = ZonedDateTime.now(ZoneId.of("Europe/Warsaw"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        String formattedTime = warsawTime.format(formatter);

        StringBuilder description = getDescription(e, additionalData, stackTrace);

        try(HttpClient client = HttpClient.newHttpClient()) {
            boolean isDuplicate = false;

            {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format("https://git.tulisiowice.top/api/v1/repos/%s/issues?q=%s", repoPath, URLEncoder.encode(errorText, StandardCharsets.UTF_8))))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "token " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String str = response.body();

                ObjectMapper mapper = new ObjectMapper();

                JsonNode rootNode = mapper.readTree(str);
                JsonNode isDuplicateNode = rootNode.at("/0");
                if (!isDuplicateNode.isMissingNode()) isDuplicate = true;
            }

            String json = String.format("""
            {
                "title": "%s",
                "body": "%s",
                "assignees": ["%s"],
                "labels": [%s]
            }
            """,
                    String.format("%s %s", errorText, formattedTime),
                    description,
                    assignee,
                    (isDuplicate) ? labelId + ", " + duplicateId : labelId);

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

    private static StringBuilder getDescription(Exception e, String[] additionalData, String stackTrace) {
        StringBuilder description = new StringBuilder(String.format("###%s Error stackTrace: \n```\n", (e.getMessage() == null) ? "" : String.format(" [%s]", e.getMessage())));
        description.append(stackTrace);
        description.append("```\n\n");

        for(String str : additionalData) {
            description.append(str);
            if(!additionalData[additionalData.length - 1].equals(str)) description.append(" ");
        }

        if(additionalData.length != 0) description.append("\n");
        description.append("Automatically reported by TuliErrorer");
        return description;
    }

    private String getErrorStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public int getDuplicateId() {
        return duplicateId;
    }

    public void setDuplicateId(int duplicateId) {
        this.duplicateId = duplicateId;
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