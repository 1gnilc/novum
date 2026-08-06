package com.gnilc.novum.i18n.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.novum.i18n.entity.dto.I18nMessageDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.novum.i18n.entity.vo.I18nMessageItemVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageVo;
import com.gnilc.novum.i18n.service.DynamicI18nMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/sys/i18n-message")
public class I18nMessageController {

    private final DynamicI18nMessageService i18nMessageService;

    public I18nMessageController(DynamicI18nMessageService i18nMessageService) {
        this.i18nMessageService = i18nMessageService;
    }

    @PostMapping("/bundle/{category}")
    public R<Map<String, Object>> getMessageBundle(
            @PathVariable("category") String category) {
        return R.success(i18nMessageService.getMessageBundle(category));
    }

    @PostMapping("/page")
    public R<PageResult<I18nMessageItemVo>> getMessagePage(
            @RequestBody I18nMessagePageDto dto) {
        return R.success(i18nMessageService.getMessagePage(dto));
    }

    @PostMapping("/categories")
    public R<List<String>> getSupportedCategories() {
        return R.success(i18nMessageService.getSupportedCategories());
    }

    @PostMapping("/values/{messageKey}")
    public R<I18nMessageVo> getMessageValues(
            @PathVariable("messageKey") String messageKey) {
        return R.success(i18nMessageService.getMessageValues(messageKey));
    }

    @PostMapping("/create")
    public R<I18nMessageVo> createMessage(
            @Valid @RequestBody I18nMessageDto dto) {
        return R.success(i18nMessageService.createMessage(dto));
    }

    @PostMapping("/save")
    public R<I18nMessageVo> saveMessage(
            @Valid @RequestBody I18nMessageDto dto) {
        return R.success(i18nMessageService.saveMessage(dto));
    }

    @PostMapping("/remove/{messageKey}")
    public R<?> removeMessage(
            @PathVariable("messageKey") String messageKey) {
        i18nMessageService.removeMessage(messageKey);
        return R.success();
    }
}
