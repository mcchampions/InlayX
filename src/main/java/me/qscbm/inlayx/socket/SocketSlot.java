package me.qscbm.inlayx.socket;

import lombok.Getter;
import lombok.Setter;

/**
 * 宝石槽位
 */
@Getter
@Setter
public class SocketSlot {
    private int index;
    private String type;
    private String gemId;
    private int start;
    private int end;

    public SocketSlot(int index, String type, String gemId, int start, int end) {
        this.index = index;
        this.type = type;
        this.gemId = gemId;
        this.start = start;
        this.end = end;
    }
}
