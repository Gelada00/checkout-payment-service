package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentGatewayServiceTest {
    @Mock
    private PaymentsRepository paymentsRepository;

    @Mock
    private BankClient bankClient;

    @InjectMocks
    private PaymentGatewayService paymentGatewayService;

    @Test
    void whenBankAuthorizesPaymentThenPaymentIsAuthorized() {
      PostPaymentRequest request = validRequest();

      when(bankClient.authorize(request)).thenReturn(true);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());

      verify(bankClient).authorize(request);
      verify(paymentsRepository).add(response);
    }

    @Test
    void whenBankDeclinesPaymentThenPaymentIsDeclined() {
      PostPaymentRequest request = validRequest();

      when(bankClient.authorize(request)).thenReturn(false);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.DECLINED, response.getStatus());

      verify(bankClient).authorize(request);
      verify(paymentsRepository).add(response);
    }

    @Test
    void whenCardNumberIsInvalidThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setCardNumber("123");

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenCardNumberContainsNonNumericCharactersThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setCardNumber("22224053432488AB");

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenExpiryMonthIsLessThanOneThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setExpiryMonth(0);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenExpiryMonthIsGreaterThanTwelveThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setExpiryMonth(13);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenPaymentIsExpiredThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setExpiryMonth(1);
      request.setExpiryYear(2020);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenCurrencyIsNotSupportedThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setCurrency("JPY");

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenAmountIsZeroThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setAmount(0);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenAmountIsNegativeThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setAmount(-100);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenCvvIsLessThanThreeDigitsThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setCvv("99");

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenCvvIsGreaterThanFourDigitsThenPaymentIsRejected() {
      PostPaymentRequest request = validRequest();
      request.setCvv("10000");

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenRequestIsNullThenPaymentIsRejected() {
      PostPaymentResponse response =
          paymentGatewayService.processPayment(null);

      assertEquals(PaymentStatus.REJECTED, response.getStatus());

      verifyNoInteractions(bankClient);
      verifyNoInteractions(paymentsRepository);
    }

    @Test
    void whenBankIsUnavailableThenAcquiringBankExceptionIsThrown() {
      PostPaymentRequest request = validRequest();

      when(bankClient.authorize(request))
          .thenThrow(
              new AcquiringBankException(
                  "Unable to communicate with acquiring bank"
              )
          );

      assertThrows(
          AcquiringBankException.class,
          () -> paymentGatewayService.processPayment(request)
      );

      verify(bankClient).authorize(request);
    }

    @Test
    void whenPaymentIsAuthorizedThenResponseContainsPaymentDetails() {
      PostPaymentRequest request = validRequest();

      when(bankClient.authorize(request)).thenReturn(true);

      PostPaymentResponse response =
          paymentGatewayService.processPayment(request);

      assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
      assertEquals(8877, response.getCardNumberLastFour());
      assertEquals(12, response.getExpiryMonth());
      assertEquals(2027, response.getExpiryYear());
      assertEquals("GBP", response.getCurrency());
      assertEquals(100, response.getAmount());

      verify(paymentsRepository).add(response);
    }

    private PostPaymentRequest validRequest() {
      PostPaymentRequest request = new PostPaymentRequest();

      request.setCardNumber("2222405343248877");
      request.setExpiryMonth(12);
      request.setExpiryYear(2027);
      request.setCurrency("GBP");
      request.setAmount(100);
      request.setCvv("123");

      return request;
    }
  }
