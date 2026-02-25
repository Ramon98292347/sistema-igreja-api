package click.rfautomatic.sistemaigreja.r2;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class R2SignedUrlService {

  private final S3Presigner presigner;
  private final String defaultBucket;

  public R2SignedUrlService(S3Presigner presigner, @Value("${app.r2.bucket}") String defaultBucket) {
    this.presigner = presigner;
    this.defaultBucket = defaultBucket;
  }

  public record SignedUrlResponse(String url, String method, Map<String, String> headers, long expires_in_seconds) {}

  public SignedUrlResponse signUpload(String bucket, String key, String contentType, long expiresSeconds) {
    String b = (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;

    PutObjectRequest.Builder req = PutObjectRequest.builder().bucket(b).key(key);
    if (contentType != null && !contentType.isBlank()) {
      req.contentType(contentType);
    }

    final PutObjectRequest putReq = req.build();
    PresignedPutObjectRequest presigned =
        presigner.presignPutObject(
            r -> r.signatureDuration(Duration.ofSeconds(expiresSeconds)).putObjectRequest(putReq));

    Map<String, String> headers = new LinkedHashMap<>();
    // Some clients must send these exactly as signed.
    if (contentType != null && !contentType.isBlank()) {
      headers.put(HttpHeaders.CONTENT_TYPE, contentType);
    }

    return new SignedUrlResponse(
        presigned.url().toString(),
        SdkHttpMethod.PUT.name(),
        headers,
        expiresSeconds);
  }

  public SignedUrlResponse signDownload(String bucket, String key, long expiresSeconds, String filename) {
    String b = (bucket == null || bucket.isBlank()) ? defaultBucket : bucket;

    GetObjectRequest.Builder req = GetObjectRequest.builder().bucket(b).key(key);
    if (filename != null && !filename.isBlank()) {
      req.responseContentDisposition("attachment; filename=\"" + filename.replace("\"", "") + "\"");
    }

    final GetObjectRequest getReq = req.build();
    PresignedGetObjectRequest presigned =
        presigner.presignGetObject(
            r -> r.signatureDuration(Duration.ofSeconds(expiresSeconds)).getObjectRequest(getReq));

    return new SignedUrlResponse(
        presigned.url().toString(),
        SdkHttpMethod.GET.name(),
        Map.of(),
        expiresSeconds);
  }
}
