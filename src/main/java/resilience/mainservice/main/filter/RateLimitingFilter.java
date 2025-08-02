package resilience.mainservice.main.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-KEY");

        // API 키가 헤더에 없는 경우 요청을 거부합니다.
        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("API Key is missing.");
            return;
        }

        // RateLimitingService를 통해 요청 허용 여부를 확인합니다.
        if (rateLimitingService.isAllowed(apiKey)) {
            // 허용되면 다음 필터로 체인을 계속 진행합니다.
            filterChain.doFilter(request, response);
        } else {
            // 제한 횟수를 초과하면 429 Too Many Requests 상태 코드를 반환합니다.
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests for this API Key.");
        }
    }
}