package resilience.mainservice.main.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    // RateLimitingService를 주입받습니다.
    private final RateLimitingService rateLimitingService;

    public FilterConfig(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();

        // RateLimitingFilter를 등록하고, 생성자에 서비스를 주입합니다.
        registrationBean.setFilter(new RateLimitingFilter(rateLimitingService));

        // 필터를 적용할 URL 패턴을 지정합니다.
        registrationBean.addUrlPatterns("/payments/*");

        return registrationBean;
    }
}