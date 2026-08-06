package com.gnilc.novum.i18n.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.i18n.dao.I18nMessageDao;
import com.gnilc.novum.i18n.entity.bo.I18nMessageBo;
import com.gnilc.novum.i18n.entity.dto.I18nMessageValueDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessageDto;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicI18nMessageServiceImplTest {

    @Mock
    private I18nMessageDao dao;

    private DynamicI18nMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(I18nMessageBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "i18n-service-test"),
                    I18nMessageBo.class);
        }
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/system/messages");
        messageSource.setDefaultEncoding("UTF-8");
        service = spy(new DynamicI18nMessageServiceImpl(new I18nMessageService(messageSource, "zh-CN")));
        ReflectionTestUtils.setField(service, "baseMapper", dao);
    }

    @Test
    void getValuesUsesMapperAndReturnsSupportedLocaleOrder() {
        stubLambdaQuery();
        when(dao.selectList(any())).thenReturn(List.of(
                row("en-US", "Home"),
                row("zh-CN", "首页")));

        var message = service.getMessageValues("menu.home.title");

        assertThat(message.getCategory()).isEqualTo("admin");
        assertThat(message.getMessageKey()).isEqualTo("menu.home.title");
        assertThat(message.getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:首页", "en-US:Home");
        verify(dao).selectList(any());
    }

    @Test
    void createPersistsANewMessage() {
        stubLambdaQuery();
        when(dao.selectList(any())).thenReturn(List.of(), List.of());
        when(dao.insert(any(I18nMessageBo.class))).thenReturn(1);

        var message = service.createMessage(save("menu.create.title",
                value("zh-CN", "新增"), value("en-US", "Create")));

        assertThat(message.getCategory()).isEqualTo("admin");
        assertThat(message.getMessageKey()).isEqualTo("menu.create.title");
        assertThat(message.getValues())
                .extracting(value -> value.getLocale() + ":" + value.getValue())
                .containsExactly("zh-CN:新增", "en-US:Create");
        verify(dao, times(2)).insert(any(I18nMessageBo.class));
    }

    @Test
    void createRejectsAnExistingMessageWithoutWriting() {
        stubLambdaQuery();
        when(dao.selectList(any())).thenReturn(
                List.of(row("en-US", "Home")),
                List.of(row("en-US", "Home")));

        assertThatThrownBy(() -> service.createMessage(save("menu.home.title",
                value("en-US", "Replacement"))))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Message key已存在");
        verify(dao, never()).insert(any(I18nMessageBo.class));
    }

    @Test
    void createTranslatesConcurrentDuplicateInsertToExistingKeyError() {
        stubLambdaQuery();
        when(dao.selectList(any())).thenReturn(List.of(), List.of());
        DuplicateKeyException duplicate = new DuplicateKeyException("duplicate key");
        when(dao.insert(any(I18nMessageBo.class))).thenThrow(duplicate);

        assertThatThrownBy(() -> service.createMessage(save("menu.concurrent.title",
                value("en-US", "Concurrent"))))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Message key已存在")
                .hasCause(duplicate);
        verify(dao).insert(any(I18nMessageBo.class));
    }

    @Test
    void saveRejectsInvalidInputBeforeDatabaseAccess() {
        I18nMessageDto duplicateLocales = save("menu.home.title",
                value("zh-CN", "首页"), value("zh-CN", "主页"));

        assertThatThrownBy(() -> service.saveMessage(duplicateLocales))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.saveMessage(save("menu.__proto__.title",
                value("en-US", "Home"))))
                .isInstanceOf(InvalidArgumentException.class);
        I18nMessageDto unknownCategory = save("menu.home.title", value("en-US", "Home"));
        unknownCategory.setCategory("unknown");
        assertThatThrownBy(() -> service.saveMessage(unknownCategory))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.saveMessage(save("menu.home.title",
                value("zh-CN", "首页"))))
                .isInstanceOf(InvalidArgumentException.class);
        assertThatThrownBy(() -> service.saveMessage(save("menu.home.title",
                value("en-US", "Home"), value("ha-NG", "Gida"))))
                .isInstanceOf(InvalidArgumentException.class);
        verifyNoInteractions(dao);
    }

    @ParameterizedTest(name = "rejects key: {0}")
    @MethodSource("invalidMessageKeys")
    void getValuesRejectsEveryInvalidMessageKeyBoundaryBeforeDatabaseAccess(
            String caseName,
            String messageKey) {
        assertThatThrownBy(() -> service.getMessageValues(messageKey))
                .isInstanceOf(InvalidArgumentException.class);
        verifyNoInteractions(dao);
    }

    private void stubLambdaQuery() {
        doAnswer(invocation -> new LambdaQueryChainWrapper<>(
                dao, Wrappers.lambdaQuery(I18nMessageBo.class)))
                .when(service).lambdaQuery();
    }

    private I18nMessageBo row(String locale, String value) {
        I18nMessageBo row = new I18nMessageBo();
        row.setCategory("admin");
        row.setMessageKey("menu.home.title");
        row.setLocale(locale);
        row.setI18nValue(value);
        return row;
    }

    private I18nMessageDto save(String key, I18nMessageValueDto... values) {
        I18nMessageDto dto = new I18nMessageDto();
        dto.setCategory("admin");
        dto.setMessageKey(key);
        dto.setValues(List.of(values));
        return dto;
    }

    private I18nMessageValueDto value(String locale, String value) {
        I18nMessageValueDto dto = new I18nMessageValueDto();
        dto.setLocale(locale);
        dto.setValue(value);
        return dto;
    }

    private static Stream<Arguments> invalidMessageKeys() {
        return Stream.of(
                Arguments.of("null", (Object) null),
                Arguments.of("blank", "   "),
                Arguments.of("leading digit", "1menu.title"),
                Arguments.of("empty segment", "menu..title"),
                Arguments.of("trailing separator", "menu.title."),
                Arguments.of("hyphen", "menu-item.title"),
                Arguments.of("newline", "menu.\ntitle"),
                Arguments.of("emoji", "menu.😀.title"),
                Arguments.of("prototype segment", "menu.prototype.title"),
                Arguments.of("constructor segment", "constructor.title"),
                Arguments.of("proto segment", "menu.__proto__"),
                Arguments.of("length 192", "a".repeat(192)));
    }
}
