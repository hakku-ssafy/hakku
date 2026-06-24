package com.hakku.payment.gateway.toss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.payment.gateway.GatewayResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link TossPaymentClient} 의 RestClient 구현. {@code POST {base}/v1/payments/confirm} 을 호출한다.
 *
 * <p>인증: 시크릿키로 Basic 인증 헤더({@code base64(secretKey + ":")})를 만든다(토스 규격). 시크릿키는 절대
 * 프론트에 노출되지 않고 서버 환경변수({@code payment.toss.secret-key})로만 주입된다.
 *
 * <p>응답 매핑: 2xx → 승인, 4xx(카드 거절 등 비즈니스 실패) → {@code failed(토스 code)}, 5xx/통신오류 → 미정이므로
 * {@link TossApiUnavailableException}.
 */
@Component
public class RestClientTossPaymentClient implements TossPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String DEFAULT_REJECT_CODE = "TOSS_CONFIRM_REJECTED";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String authHeader;

    public RestClientTossPaymentClient(RestClient.Builder builder, ObjectMapper objectMapper,
            @Value("${payment.toss.base-url}") String baseUrl,
            @Value("${payment.toss.secret-key}") String secretKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayResult confirm(String paymentKey, String orderId, long amount) {
        try {
            restClient.post()
                    .uri(CONFIRM_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                    .retrieve()
                    .toBodilessEntity();
            return GatewayResult.approved(paymentKey);
        } catch (HttpClientErrorException ex) {
            return GatewayResult.failed(extractCode(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            throw new TossApiUnavailableException("토스 결제 승인 통신에 실패했습니다.", ex);
        }
    }

    /** 토스 에러 응답 바디에서 {@code code} 를 추출한다. 파싱 불가하면 일반화 코드로 대체한다. */
    private String extractCode(String body) {
        if (body == null || body.isBlank()) {
            return DEFAULT_REJECT_CODE;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String code = node.path("code").asText("");
            return code.isBlank() ? DEFAULT_REJECT_CODE : code;
        } catch (Exception e) {
            return DEFAULT_REJECT_CODE;
        }
    }
}
