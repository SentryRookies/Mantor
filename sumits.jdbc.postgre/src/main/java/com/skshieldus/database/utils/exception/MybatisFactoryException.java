package com.skshieldus.database.utils.exception;

/**
 * create on 2023-04-17.
 * <p> MybatisFactory 에서 발생하는 예외 처리 </p>
 * @author sjoh14
 * @version 1.0
 */
public class MybatisFactoryException extends RuntimeException {

    public MybatisFactoryException(String message) {
        super(message);
    }
}

