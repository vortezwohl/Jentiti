package org.jentiti.error;

public class NoSuchEntityException extends RuntimeException {

    public NoSuchEntityException(String msg){
        super(msg);
    }
}
