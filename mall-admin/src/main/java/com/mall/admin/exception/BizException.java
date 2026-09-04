package com.mall.admin.exception;

public class BizException extends RuntimeException {

    private int code = 500;

    public BizException(String msg) {
        super(msg);
    }

    public BizException(String msg, Throwable e) {
        super(msg, e);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
