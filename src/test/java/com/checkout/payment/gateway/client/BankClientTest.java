package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class BankClientTest {

  @Mock
  private RestTemplate restTemplate;

  private BankClient bankClient;

  @BeforeEach
  void setUp() {
    bankClient = new BankClient(
        restTemplate,
        "http://localhost:8080/payments"
    );
  }

  @Test
  void whenBankAuthorizesPaymentThenTrueIsReturned() {
    PostPaymentRequest request = createRequest();

    Map<String, Object> bankResponse = Map.of(
        "authorized", true,
        "authorization_code", "123456"
    );

    when(restTemplate.postForObject(
        eq("http://localhost:8080/payments"),
        anyMap(),
        eq(Map.class)
    )).thenReturn(bankResponse);

    boolean result = bankClient.authorize(request);

    assertTrue(result);
  }

  private PostPaymentRequest createRequest() {
    PostPaymentRequest request = new PostPaymentRequest();

    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");

    return request;
  }

  @Test
  void whenBankDeclinesPaymentThenFalseIsReturned() {
    PostPaymentRequest request = createRequest();

    Map<String, Object> bankResponse = Map.of(
        "authorized", false,
        "authorization_code", ""
    );

    when(restTemplate.postForObject(
        eq("http://localhost:8080/payments"),
        anyMap(),
        eq(Map.class)
    )).thenReturn(bankResponse);

    boolean result = bankClient.authorize(request);

    assertFalse(result);
  }

  @Test
  void whenBankCallFailsThenAcquiringBankExceptionIsThrown() {
    PostPaymentRequest request = createRequest();

    when(restTemplate.postForObject(
        eq("http://localhost:8080/payments"),
        anyMap(),
        eq(Map.class)
    )).thenThrow(new RestClientException("Bank unavailable"));

    assertThrows(
        AcquiringBankException.class,
        () -> bankClient.authorize(request)
    );
  }
}
