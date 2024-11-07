package com.rickey.common.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @TableName order
 */
@TableName(value = "order")
@Data
public class Order implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建订单的用户
     */
    private Long userId;

    /**
     * 用户要购买的接口
     */
    private Long interfaceId;

    /**
     * 购买的调用次数
     */
    private Integer quantity;

    /**
     * 总价格
     */
    private BigDecimal totalPrice;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除(0-未删, 1-已删)
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}