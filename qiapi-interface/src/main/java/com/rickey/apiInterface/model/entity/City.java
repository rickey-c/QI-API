package com.rickey.apiInterface.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "city")
@Data
public class City {

    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    String name;

    String adCode;

    String cityCode;

}
