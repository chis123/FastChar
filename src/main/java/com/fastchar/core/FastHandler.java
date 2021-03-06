package com.fastchar.core;

public final class FastHandler extends FastBaseInfo {
    private static final long serialVersionUID = 5137431042772520606L;

    public int getCode() {
        return getInt("code", 0);
    }

    public FastHandler setCode(int code) {
        put("code", code);
        return this;
    }

    public String getError() {
        return getString("error");
    }

    public FastHandler setError(String error) {
        put("error", error);
        return this;
    }

}
