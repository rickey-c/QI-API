package com.rickey.apiInterface.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rickey.apiInterface.mapper.CityMapper;
import com.rickey.apiInterface.model.entity.City;
import com.rickey.apiInterface.service.CityService;
import org.springframework.stereotype.Service;

@Service
public class CityServiceImpl extends ServiceImpl<CityMapper, City>
        implements CityService {

}
