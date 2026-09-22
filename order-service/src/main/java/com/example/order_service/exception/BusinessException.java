package com.example.order_service.exception;

/**
 * Excepción base de negocio con código y estado HTTP asociados.
 */
public class BusinessException extends RuntimeException {

    private final int httpStatus;
    private final String code;

    public BusinessException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
