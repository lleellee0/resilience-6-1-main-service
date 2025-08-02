package resilience.mainservice.main.mail;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter; // RateLimiter 임포트
import io.github.resilience4j.bulkhead.annotation.Bulkhead; // Bulkhead 임포트

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import resilience.mainservice.main.circuitbreaker.MailServiceTimeoutException;
import java.time.Duration;

@Component
public class MailServiceClientWebClient implements MailServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MailServiceClientWebClient.class);
    private final WebClient webClient;

    private static final String MAIL_SERVICE_RL = "mailServiceRateLimit"; // RateLimiter 이름 정의
    private static final String MAIL_SERVICE_BH = "mailServiceBulkhead"; // Bulkhead 이름 정의
    private static final String MAIL_SEND_URI = "/mail/send";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public MailServiceClientWebClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    @Override
    @RateLimiter(name = MAIL_SERVICE_RL, fallbackMethod = "sendMailFallback")
//    @Bulkhead(name = MAIL_SERVICE_BH, fallbackMethod = "sendMailFallback")
    public String sendMail(String email) {
        log.debug("[Resilience] Attempting to send mail for email: {}", email);
        EmailRequest request = new EmailRequest(email);

        return webClient.post()
                .uri(MAIL_SEND_URI)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.warn("[Resilience] Mail service returned 5xx server error for {}: {}", email, errorBody);
                                    // 5xx 에러는 RateLimiter/Bulkhead 실패로 기록되지 않도록 별도 처리 (선택사항)
                                    return Mono.error(new MailSendException("Mail service server error: " + errorBody));
                                })
                )
                .bodyToMono(String.class)
                .timeout(DEFAULT_TIMEOUT, Mono.error(
                        () -> new MailServiceTimeoutException("Timeout after " + DEFAULT_TIMEOUT + " for " + email))
                )
                .doOnSuccess(response -> log.debug("[Resilience] Successfully sent mail for {}", email))
                .block(); // 동기 처리 유지
    }

    /**
     * sendMail 메소드의 Fallback 메소드.
     * RateLimiter 제한 초과 (RequestNotPermitted), Bulkhead 제한 초과 (BulkheadFullException),
     * 또는 설정에 따라 다른 예외 발생 시 호출됩니다.
     */
    public String sendMailFallback(String email, Throwable t) {
        // 어떤 예외로 Fallback이 실행되었는지 로깅 (RateLimiter, Bulkhead 예외 포함)
        log.warn("[Resilience Fallback] Fallback activated for email: {}. Reason: {}",
                email, t.getClass().getSimpleName() + ": " + t.getMessage());

        // Fallback 로직
        // 유저에 의한 호출이었다면, 추상적인 예외를 만들어서 클라이언트에게 횟수 제한을 초과했다던지 알려주면 좋음.
        throw new RuntimeException("Mail service temporarily unavailable or limit exceeded. Fallback executed for " + email + ". Cause: " + t.getMessage(), t);
    }
}