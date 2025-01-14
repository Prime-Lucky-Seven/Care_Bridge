package org.example.carebridge.global.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.sasl.AuthenticationException;
import java.util.HashMap;
import java.util.Map;

public class GlobalExceptionHandler {



    //JWT 예외 처리
    //...
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ExceptionResponseBody> handleAuthenticationException(AuthenticationException e) {
        HttpStatus httpStatus = e instanceof AuthenticationException
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.FORBIDDEN;

        // 에러 메시지를 Map으로 구성
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("message", e.getMessage());

        // ExceptionResponseBody 생성
        ExceptionResponseBody responseBody = new ExceptionResponseBody(
                httpStatus,
                httpStatus.value(),
                errorDetails
        );

        // ResponseEntity 반환
        return ResponseEntity
                .status(httpStatus)
                .body(responseBody);

//        HttpStatus statusCode = e instanceof AuthenticationException ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;

//        return ResponseEntity
//                .status(statusCode)
//                .body(new ExceptionResponseBody (e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ExceptionResponseBody> handleAccessDeniedException(AccessDeniedException e) {
        ExceptionResponseBody exceptionResponseBody = new ExceptionResponseBody(
                HttpStatus.FORBIDDEN,
                HttpStatus.FORBIDDEN.value(),
                Map.of("Access-Dined", "사용자 권한 부족")
        );

        return new ResponseEntity<>(exceptionResponseBody, HttpStatus.FORBIDDEN);

    }

    @ExceptionHandler(JwtException.class)
    protected ResponseEntity<ExceptionResponseBody> handleJwtException(JwtException e) {
        HttpStatus httpStatus = e instanceof ExpiredJwtException
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.FORBIDDEN;

        // 에러 메시지를 Map으로 구성
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("message", e.getMessage());

        // ExceptionResponseBody 생성
        ExceptionResponseBody responseBody = new ExceptionResponseBody(
                httpStatus,
                httpStatus.value(),
                errorDetails
        );

        // ResponseEntity 반환
        return ResponseEntity
                .status(httpStatus)
                .body(responseBody);
    }



}
