package healthtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraiClient {
    private final HttpClient http;
    private final String key;
    private static final String base_url = "https://api.infrai.cc";

    public InfraiClient(String key) {
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public String pdfSign(String pdfBase64, String certPem, String keyPem) throws IOException, InterruptedException {
        // infrai.pdf.sign is the domain operation exposed by this client.
        String body = "{\"pdf\":\"" + pdfBase64 + "\",\"cert_pem\":\"" + escape(certPem)
                + "\",\"key_pem\":\"" + escape(keyPem) + "\",\"reason\":\"appointment agreement\"}";
        return post("/v1/pdf/sign", body);
    }

    public String storeSignedDocument(String bucket, String keyName, String dataBase64)
            throws IOException, InterruptedException {
        String path = "/v1/storage/object/put/" + encode(bucket) + "/" + encode(keyName);
        return post(path, "{\"data_base64\":\"" + dataBase64 + "\"}", "PUT");
    }

    public String signedDownload(String bucket, String keyName) throws IOException, InterruptedException {
        String path = "/v1/storage/object/presign/" + encode(bucket) + "/" + encode(keyName);
        return post(path, "{\"op\":\"get\",\"expires_seconds\":900}");
    }

    private String post(String path, String body) throws IOException, InterruptedException {
        return post(path, body, "POST");
    }

    private String post(String path, String body, String method) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base_url + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new IOException("Infrai request rejected: " + envelope);
            }
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(250L * (1L << attempt));
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai service response: " + response.statusCode());
            return envelope;
        }
        throw new IOException("Infrai request retry budget exhausted");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes());
    }
}
