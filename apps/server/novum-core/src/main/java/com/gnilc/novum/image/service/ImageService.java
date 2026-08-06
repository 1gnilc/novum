package com.gnilc.novum.image.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageParams;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.image.entity.bo.ImageBo;
import com.gnilc.novum.image.entity.vo.ImagePresignVo;
import com.gnilc.novum.image.entity.vo.ImageVo;

public interface ImageService extends IService<ImageBo> {
    ImagePresignVo presign(String contentType, Long contentLength);

    ImageVo finalize(String objectKey);

    PageResult<ImageVo> getImagePage(PageParams params);

    void removeImage(Long id);

    String getUrl(String objectKey);
}
