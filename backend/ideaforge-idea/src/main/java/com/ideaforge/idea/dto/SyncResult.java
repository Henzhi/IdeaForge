package com.ideaforge.idea.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SyncResult {
    /** 同步成功的想法(服务端最新状态) */
    private List<IdeaResp> synced;
    /** 冲突的 client_uuid 列表(客户端版本较旧) */
    private List<String> conflicts;
}
