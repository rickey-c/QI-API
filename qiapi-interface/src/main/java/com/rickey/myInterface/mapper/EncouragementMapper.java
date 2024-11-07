package com.rickey.myInterface.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.myInterface.model.entity.Encouragement;

/**
 * 接口信息
 */
public interface EncouragementMapper extends BaseMapper<Encouragement> {

    Encouragement getRandomEncouragement();
}




