package resilience.mainservice.main.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    // 동시성 문제를 해결하기 위해 ConcurrentHashMap 사용
    private final Map<String, RequestData> requestCounts = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitingService(@Value("${api.requests-per-minute}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    /**
     * API 키에 대한 요청이 허용되는지 확인합니다.
     * @param apiKey 요청을 보낸 API 키
     * @return 요청이 허용되면 true, 아니면 false
     */
    public boolean isAllowed(String apiKey) {
        // 현재 시간을 가져옵니다.
        long currentTime = System.currentTimeMillis();

        // compute 메소드를 사용하여 원자적으로(atomically) 맵을 업데이트합니다.
        RequestData requestData = requestCounts.compute(apiKey, (key, data) -> {
            if (data == null || isWindowExpired(data, currentTime)) {
                // 데이터가 없거나, 1분(윈도우)이 지났으면 새로 생성
                return new RequestData(1, currentTime);
            }
            // 윈도우가 유효하면 카운트 증가
            data.incrementCount();
            return data;
        });

        // 최대 요청 횟수를 초과했는지 확인합니다.
        return requestData.getCount() <= requestsPerMinute;
    }

    /**
     * 현재 요청이 1분 윈도우를 벗어났는지 확인합니다.
     */
    private boolean isWindowExpired(RequestData data, long currentTime) {
        // (현재 시간 - 첫 요청 시간)이 60초(60000ms)를 초과했는지 확인
        return (currentTime - data.getFirstRequestTime()) > 60000;
    }

    /**
     * 각 API 키의 요청 데이터를 저장하는 내부 클래스
     */
    private static class RequestData {
        private int count;
        private final long firstRequestTime;

        public RequestData(int count, long firstRequestTime) {
            this.count = count;
            this.firstRequestTime = firstRequestTime;
        }

        public int getCount() {
            return count;
        }

        public long getFirstRequestTime() {
            return firstRequestTime;
        }

        public void incrementCount() {
            this.count++;
        }
    }
}