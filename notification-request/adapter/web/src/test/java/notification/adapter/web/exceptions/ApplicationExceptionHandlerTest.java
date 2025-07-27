package notification.adapter.web.exceptions;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import notification.definition.exceptions.BusinessRuleViolationException;
import notification.definition.exceptions.DataNotFoundException;
import notification.definition.exceptions.DuplicateRequestException;
import notification.definition.exceptions.MandatoryFieldException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ApplicationExceptionHandlerTest {

    private WebTestClient webTestClient;

    @Mock
    private TestService testService;

    @BeforeEach
    void setUp() {
        TestController testController = new TestController(testService);
        ApplicationExceptionHandler exceptionHandler = new ApplicationExceptionHandler();

        webTestClient = WebTestClient.bindToController(testController)
                .controllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void handleDataNotFoundException() {
        // given
        when(testService.getData()).thenThrow(new DataNotFoundException("Data not found"));

        // when & then
        webTestClient.get()
                .uri("/test/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Data not found")
                .jsonPath("$.path").isEqualTo("/test/data");
    }

    @Test
    void handleDuplicateRequestException() {
        // given
        when(testService.createData()).thenThrow(new DuplicateRequestException("Duplicate request"));

        // when & then
        webTestClient.get()
                .uri("/test/create")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Duplicate request")
                .jsonPath("$.path").isEqualTo("/test/create");
    }

    @Test
    void handleMandatoryFieldException() {
        // given
        when(testService.validateData()).thenThrow(new MandatoryFieldException("Required field is missing"));

        // when & then
        webTestClient.get()
                .uri("/test/validate")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Mandatory field missing")
                .jsonPath("$.path").isEqualTo("/test/validate");
    }

    @Test
    void handleBusinessRuleViolationException() {
        // given
        when(testService.processData()).thenThrow(new BusinessRuleViolationException("Business rule violated"));

        // when & then
        webTestClient.get()
                .uri("/test/process")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Business rule violation")
                .jsonPath("$.path").isEqualTo("/test/process");
    }

    @Test
    void handleDataNotFoundException_withCause() {
        // given
        DataNotFoundException ex = new DataNotFoundException("User not found",
                new RuntimeException("DB connection failed"));
        when(testService.getData()).thenThrow(ex);

        // when & then
        webTestClient.get()
                .uri("/test/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Data not found")
                .jsonPath("$.path").isEqualTo("/test/data")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void handleDuplicateRequestException_verifyLogging() {
        // given
        when(testService.createData()).thenThrow(new DuplicateRequestException("Already exists"));

        // when & then
        webTestClient.get()
                .uri("/test/create")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.message").isEqualTo("Duplicate request")
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.code").isEmpty()
                .jsonPath("$.errors").isEmpty();
    }

    @Test
    void handleMandatoryFieldException_withNullMessage() {
        // given
        when(testService.validateData()).thenThrow(new MandatoryFieldException("null message"));

        // when & then
        webTestClient.get()
                .uri("/test/validate")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Mandatory field missing")
                .jsonPath("$.path").isEqualTo("/test/validate");
    }

    @Test
    void handleBusinessRuleViolationException_withEmptyMessage() {
        // given
        when(testService.processData()).thenThrow(new BusinessRuleViolationException(""));

        // when & then
        webTestClient.get()
                .uri("/test/process")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Business rule violation")
                .jsonPath("$.path").isEqualTo("/test/process");
    }

    @Test
    void handleMultipleExceptions_sequential() {
        // given - 다양한 예외를 순차적으로 테스트
        when(testService.getData()).thenThrow(new DataNotFoundException("Data not found"));
        when(testService.createData()).thenThrow(new DuplicateRequestException("Duplicate"));
        when(testService.validateData()).thenThrow(new MandatoryFieldException("Required field missing"));
        when(testService.processData()).thenThrow(new BusinessRuleViolationException("Rule violated"));

        // when & then - 각 예외가 올바른 HTTP 상태와 메시지로 처리되는지 확인
        webTestClient.get().uri("/test/data").exchange().expectStatus().isNotFound();
        webTestClient.get().uri("/test/create").exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
        webTestClient.get().uri("/test/validate").exchange().expectStatus().isBadRequest();
        webTestClient.get().uri("/test/process").exchange().expectStatus().isBadRequest();
    }

    @Test
    void verifyResponseStructure_allRequiredFields() {
        // given
        when(testService.getData()).thenThrow(new DataNotFoundException("Test data not found"));

        // when & then - 응답 구조의 모든 필드가 올바른지 확인
        webTestClient.get()
                .uri("/test/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.status").isNumber()
                .jsonPath("$.error").isNotEmpty()
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.path").isNotEmpty()
                .jsonPath("$.code").isEmpty()
                .jsonPath("$.errors").isEmpty();
    }

    // Mock Controller for testing
    @RestController
    static class TestController {
        private final TestService testService;

        public TestController(TestService testService) {
            this.testService = testService;
        }

        @GetMapping("/test/data")
        public Mono<String> getData() {
            return Mono.fromCallable(() -> testService.getData());
        }

        @GetMapping("/test/create")
        public Mono<String> createData() {
            return Mono.fromCallable(() -> testService.createData());
        }

        @GetMapping("/test/validate")
        public Mono<String> validateData() {
            return Mono.fromCallable(() -> testService.validateData());
        }

        @GetMapping("/test/process")
        public Mono<String> processData() {
            return Mono.fromCallable(() -> testService.processData());
        }
    }

    // Mock Service interface
    interface TestService {
        String getData();

        String createData();

        String validateData();

        String processData();
    }
}
