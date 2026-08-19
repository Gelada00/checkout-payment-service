package com.checkout.payment.gateway.exception;

public class AcquiringBankException extends RuntimeException{
  public AcquiringBankException(String message) {
    super(message);
  }
}
