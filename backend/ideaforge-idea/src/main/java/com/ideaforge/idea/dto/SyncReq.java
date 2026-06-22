package com.ideaforge.idea.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class SyncReq {
    @Valid
    private List<IdeaUpsertReq> upserts;

    /** 待删除的 client_uuid 列表 */
    private List<String> deletes;
}
