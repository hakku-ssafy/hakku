package com.hakku.payment.gateway.toss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.payment.config.RestClientConfig;
import com.hakku.payment.gateway.GatewayResult;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 토스 결제 승인(confirm) HTTP 클라이언트 검증(MockRestServiceServer, 실 네트워크 없음).
 * v2 위젯 SDK 도 서버 승인은 v1 REST API({@code POST /v1/payments/confirm})를 호출한다.
 * 2xx → 승인, 4xx → 거절(코드 추출), 5xx/통신오류 → 미정(예외)로 매핑되는지 본다.
 */
class RestClientTossPaymentClientTest {

    private static final String BASE_URL = "https://api.tosspayments.com";
    private static final String SECRET_KEY = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";

    private MockRestServiceServer server;
    private RestClientTossPaymentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientTossPaymentClient(builder, new ObjectMapper(), BASE_URL, SECRET_KEY);
    }

    private String expectedBasicAuth() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("2xx 응답이면 승인 결과(providerTxId=paymentKey)로 매핑한다 — Basic 인증/바디 포함")
    void approvesOnSuccess() {
        server.expect(requestTo(BASE_URL + "/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, expectedBasicAuth()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentKey").value("pk_1"))
                .andExpect(jsonPath("$.orderId").value("order_1"))
                .andExpect(jsonPath("$.amount").value(15000))
                .andRespond(withSuccess("{\"status\":\"DONE\",\"paymentKey\":\"pk_1\"}",
                        MediaType.APPLICATION_JSON));

        GatewayResult result = client.confirm("pk_1", "order_1", 15000L);

        assertTrue(result.approved());
        assertEquals("pk_1", result.providerTxId());
        server.verify();
    }

    @Test
    @DisplayName("4xx 응답이면 거절 결과로 매핑하고 토스 에러 code 를 사유로 담는다")
    void failsOnClientError() {
        server.expect(requestTo(BASE_URL + "/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"code\":\"REJECT_CARD_COMPANY\",\"message\":\"카드사 거절\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        GatewayResult result = client.confirm("pk_2", "order_2", 15000L);

        assertFalse(result.approved());
        assertEquals("REJECT_CARD_COMPANY", result.failureReason());
    }

    @Test
    @DisplayName("5xx/통신 오류는 미정 상태이므로 정산하지 않고 TossApiUnavailableException 으로 올린다")
    void throwsOnServerError() {
        server.expect(requestTo(BASE_URL + "/v1/payments/confirm"))
                .andRespond(withServerError());

        assertThrows(TossApiUnavailableException.class,
                () -> client.confirm("pk_3", "order_3", 15000L));
    }

    @Test
    @DisplayName("H-1: 응답 지연이 read timeout 을 넘으면 TossApiUnavailableException — 워커 스레드 무한 점유 방지")
    void readTimeoutMapsToUnavailable() throws Exception {
        // 연결은 받아주되 응답을 보내지 않는 로컬 서버 → read timeout 을 강제 유발.
        try (ServerSocket stalling = new ServerSocket(0)) {
            Thread acceptor = new Thread(() -> {
                try (Socket s = stalling.accept()) {
                    Thread.sleep(2_000);
                } catch (Exception ignored) {
                    // accept/sleep 중단은 테스트 종료 정리 과정 — 무시.
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            String baseUrl = "http://localhost:" + stalling.getLocalPort();
            RestClient.Builder timed = RestClient.builder()
                    .requestFactory(RestClientConfig.requestFactory(1_000, 300)); // connect 1s / read 300ms
            RestClientTossPaymentClient timeoutClient =
                    new RestClientTossPaymentClient(timed, new ObjectMapper(), baseUrl, SECRET_KEY);

            assertThrows(TossApiUnavailableException.class,
                    () -> timeoutClient.confirm("pk_to", "order_to", 15000L));
        }
    }
}
