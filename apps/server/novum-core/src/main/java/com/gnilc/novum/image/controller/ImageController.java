package com.gnilc.novum.image.controller;

import com.alibaba.fastjson2.JSONObject;
import com.gnilc.common.utils.PageParams;
import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.novum.image.entity.vo.ImagePresignVo;
import com.gnilc.novum.image.entity.vo.ImageVo;
import com.gnilc.novum.image.service.ImageService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/presign")
    public R<ImagePresignVo> presign(@RequestBody JSONObject body) {
        String contentType = body.getString("contentType");
        Long contentLength = body.getLong("contentLength");
        return R.success(imageService.presign(contentType, contentLength));
    }

    @PostMapping("/finalize")
    public R<ImageVo> finalize(@RequestBody JSONObject body) {
        return R.success(imageService.finalize(body.getString("objectKey")));
    }

    @PostMapping("/page")
    public R<PageResult<ImageVo>> getImagePage(@RequestBody JSONObject body) {
        PageParams params = new PageParams();
        params.setCurrentPage(body.getLong("currentPage"));
        params.setPageSize(body.getLong("pageSize"));
        return R.success(imageService.getImagePage(params));
    }

    @PostMapping("/remove/{id}")
    public R<?> removeImage(@PathVariable("id") Long id) {
        imageService.removeImage(id);
        return R.success();
    }
}
