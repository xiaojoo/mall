package com.mall.ware.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id;// 采购单id

    private List<PurchaseItemDoneVo> items;
}
