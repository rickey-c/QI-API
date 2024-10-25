package com.rickey.qiapiinterface.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.qiapiinterface.model.entity.Encouragement;

/**
 * 接口信息
 */
public interface EncouragementMapper extends BaseMapper<Encouragement> {

    Encouragement getRandomEncouragement();
}




