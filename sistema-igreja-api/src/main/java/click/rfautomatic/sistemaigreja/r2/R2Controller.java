package click.rfautomatic.sistemaigreja.r2;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/r2")
public class R2Controller {

  private final R2SignedUrlService r2;

  public R2Controller(R2SignedUrlService r2) {
    this.r2 = r2;
  }

  public record SignUploadRequest(String bucket, String key, String content_type, Long expires_seconds) {}

  @PostMapping("/sign-upload")
  @ResponseStatus(HttpStatus.CREATED)
  public R2SignedUrlService.SignedUrlResponse signUpload(@RequestBody SignUploadRequest body) {
    long exp = body.expires_seconds() == null ? 900 : body.expires_seconds();
    return r2.signUpload(body.bucket(), body.key(), body.content_type(), exp);
  }

  public record SignDownloadRequest(String bucket, String key, String filename, Long expires_seconds) {}

  @PostMapping("/sign-download")
  public R2SignedUrlService.SignedUrlResponse signDownload(@RequestBody SignDownloadRequest body) {
    long exp = body.expires_seconds() == null ? 900 : body.expires_seconds();
    return r2.signDownload(body.bucket(), body.key(), exp, body.filename());
  }
}
