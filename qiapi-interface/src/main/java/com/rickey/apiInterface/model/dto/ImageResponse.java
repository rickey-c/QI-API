package com.rickey.apiInterface.model.dto;

import lombok.Data;

@Data
public class ImageResponse {
    private String code;
    private String imgurl;
    private String width;
    private String height;
}