package resilience.mainservice.main.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    /**
     * 성공 시 항상 "정상 처리 완료" 메시지를 반환합니다.
     */
    @PostMapping("/process-mvc")
    public ResponseEntity<String> processPaymentMvc(@RequestBody PaymentRequest request) {

        // 로직 실행 후, 결과와 상관없이 고정된 성공 메시지를 200 OK 상태와 함께 반환합니다.
        return ResponseEntity.ok("정상 처리 완료");
    }
}