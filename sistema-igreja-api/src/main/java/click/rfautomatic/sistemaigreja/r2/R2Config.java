package click.rfautomatic.sistemaigreja.r2;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class R2Config {

  @Bean
  public S3Presigner r2Presigner(
      @Value("${app.r2.endpoint}") String endpoint,
      @Value("${app.r2.accessKeyId}") String accessKeyId,
      @Value("${app.r2.secretAccessKey}") String secretAccessKey,
      @Value("${app.r2.region:auto}") String region) {

    return S3Presigner.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .build();
  }
}
