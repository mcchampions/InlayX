package me.qscbm.inlayx.socket;

import lombok.Getter;

/**
 * 提取 / 移除宝石操作结果
 */
@Getter
public final class ExtractResult {
    public enum Status {
        SUCCESS,

        FAILED,

        NOT_FOUND,

        CANCELLED
    }

    private final Status status;
    private final String gemId;

    private ExtractResult(Status status, String gemId) {
        this.status = status;
        this.gemId = gemId;
    }

    public static ExtractResult success(String gemId) {
        return new ExtractResult(Status.SUCCESS, gemId);
    }

    public static ExtractResult failed(String gemId) {
        return new ExtractResult(Status.FAILED, gemId);
    }

    public static ExtractResult notFound() {
        return new ExtractResult(Status.NOT_FOUND, null);
    }

    public static ExtractResult cancelled(String gemId) {
        return new ExtractResult(Status.CANCELLED, gemId);
    }
}
