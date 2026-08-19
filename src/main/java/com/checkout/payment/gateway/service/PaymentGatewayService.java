package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Set<String> SUPPORTED_CURRENCIES =
      Set.of("GBP", "USD", "EUR");

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;

  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    if (!isValid(request)) {
      return createRejectedResponse();
    }
    boolean authorized = bankClient.authorize(request);

    PostPaymentResponse payment = createPaymentResponse(request, authorized);

    paymentsRepository.add(payment);
    return payment;
  }

  private PostPaymentResponse createRejectedResponse() {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setStatus(PaymentStatus.REJECTED);
    return response;
  }

  private PostPaymentResponse createPaymentResponse (PostPaymentRequest request, boolean authorized) {
    PostPaymentResponse response = new PostPaymentResponse();

    response.setId(UUID.randomUUID());

    if (authorized) {
      response.setStatus(PaymentStatus.AUTHORIZED);
    } else {
      response.setStatus(PaymentStatus.DECLINED);
    }

    response.setCardNumberLastFour(
        Integer.parseInt(request.getCardNumber().substring(
            request.getCardNumber().length() -4
        ))
    );

    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());

    return response;
  }


  private boolean isValid(PostPaymentRequest request) {
    return request != null
        && isValidCardNumber(request.getCardNumber())
        && isValidExpiryDate(
        request.getExpiryMonth(),
        request.getExpiryYear())
        && isValidCurrency(request.getCurrency())
        && request.getAmount() > 0
        && isValidCvv(request.getCvv());
  }

  private boolean isValidCardNumber(String cardNumber) {
    return cardNumber != null
        && cardNumber.matches("\\d{14,19}");
  }

  private boolean isValidExpiryDate(int expiryMonth, int expiryYear) {
    if (expiryMonth < 1 || expiryMonth > 12) {
      return false;
    }

    try {
      YearMonth expiryDate = YearMonth.of(expiryYear, expiryMonth);
      return expiryDate.isAfter(YearMonth.now());
    } catch (DateTimeException e) {
      return false;
    }
  }

  private boolean isValidCurrency(String currency) {
    return SUPPORTED_CURRENCIES.contains(currency);
  }

  private boolean isValidCvv(String cvv) {
    return cvv != null && cvv.matches("\\d{3,4}");
  }
}
