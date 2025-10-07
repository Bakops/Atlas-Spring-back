package com.example.operation_atlas.model;

public class PuzzleResult {
    private boolean success;
    private String errorCode;
    private String message;
    private String fragment;

    public PuzzleResult(boolean success, String errorCode, String message) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
    }

    public PuzzleResult(boolean success, String fragment) {
        this.success = success;
        this.fragment = fragment;
    }

    public static PuzzleResult success(String fragment) {
        return new PuzzleResult(true, fragment);
    }

    public static PuzzleResult error(String errorCode, String message) {
        return new PuzzleResult(false, errorCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
}

