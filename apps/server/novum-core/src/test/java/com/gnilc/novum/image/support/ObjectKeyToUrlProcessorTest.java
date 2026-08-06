package com.gnilc.novum.image.support;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.R;
import com.gnilc.novum.image.annotation.ObjectKeyToUrl;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import com.gnilc.novum.image.entity.vo.ImageVo;
import com.gnilc.novum.image.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectKeyToUrlProcessorTest {
    private final ImageService imageService = mock(ImageService.class);
    private final ObjectKeyToUrlProcessor processor = new ObjectKeyToUrlProcessor(imageService);

    @Test
    void processesAnnotatedFieldsAcrossNestedResponseShapes() {
        AnnotatedView first = new AnnotatedView("images/first.png");
        AnnotatedView second = new AnnotatedView("images/second.png");
        when(imageService.getUrl("images/first.png")).thenReturn("https://images.test/images/first.png");
        when(imageService.getUrl("images/second.png")).thenReturn("https://images.test/images/second.png");
        Object body = new Object[]{
                R.success(new PageResult<>(List.of(first), 1, 10, 1)),
                Map.of("nested", Optional.of(second))
        };

        processor.process(body);

        assertThat(first.getUrl()).isEqualTo("https://images.test/images/first.png");
        assertThat(second.getUrl()).isEqualTo("https://images.test/images/second.png");
        verify(imageService).getUrl("images/first.png");
        verify(imageService).getUrl("images/second.png");
    }

    @Test
    void supportsEverySpringMvcResponseBodyConverter() {
        assertThat(processor.supports(mock(MethodParameter.class), StringHttpMessageConverter.class)).isTrue();
    }

    @Test
    void processesFieldsWithoutInvokingApplicationGetters() {
        AnnotatedView view = new AnnotatedView("images/field.png");
        when(imageService.getUrl("images/field.png")).thenReturn("https://images.test/images/field.png");

        processor.process(new FieldOnlyContainer(view));

        assertThat(view.getUrl()).isEqualTo("https://images.test/images/field.png");
    }

    @Test
    void processesReferenceAndLazyResponseShapesWithoutConsumingThemEarly() {
        AnnotatedView referenced = new AnnotatedView("images/reference.png");
        AnnotatedView iterated = new AnnotatedView("images/iterator.png");
        AnnotatedView streamed = new AnnotatedView("images/stream.png");
        when(imageService.getUrl("images/reference.png"))
                .thenReturn("https://images.test/images/reference.png");
        when(imageService.getUrl("images/iterator.png"))
                .thenReturn("https://images.test/images/iterator.png");
        when(imageService.getUrl("images/stream.png"))
                .thenReturn("https://images.test/images/stream.png");

        AtomicReference<AnnotatedView> reference = new AtomicReference<>(referenced);
        processor.process(reference);
        Iterator<AnnotatedView> sourceIterator = List.of(iterated).iterator();
        Object iteratorBody = beforeBodyWrite(sourceIterator);
        Object streamBody = beforeBodyWrite(Stream.of(streamed));

        assertThat(referenced.getUrl()).isEqualTo("https://images.test/images/reference.png");
        assertThat(iterated.getUrl()).isNull();
        assertThat(streamed.getUrl()).isNull();
        Iterator<?> processedIterator = (Iterator<?>) iteratorBody;
        assertThat(processedIterator.next()).isSameAs(iterated);
        assertThat(processedIterator.hasNext()).isFalse();
        List<?> processedStream = ((Stream<?>) streamBody).toList();
        assertThat(processedStream).hasSize(1);
        assertThat(processedStream.get(0)).isSameAs(streamed);
        assertThat(iterated.getUrl()).isEqualTo("https://images.test/images/iterator.png");
        assertThat(streamed.getUrl()).isEqualTo("https://images.test/images/stream.png");
    }

    @Test
    void processesLazyShapesNestedInCollectionsAndFinalFields() {
        AnnotatedView listed = new AnnotatedView("images/listed.png");
        AnnotatedView nested = new AnnotatedView("images/nested.png");
        when(imageService.getUrl("images/listed.png")).thenReturn("https://images.test/images/listed.png");
        when(imageService.getUrl("images/nested.png")).thenReturn("https://images.test/images/nested.png");

        Object listBody = beforeBodyWrite(List.of(Stream.of(listed)));
        FinalIteratorContainer container = new FinalIteratorContainer(List.of(nested).iterator());
        processor.process(container);

        Object nestedStream = ((List<?>) listBody).get(0);
        List<?> nestedItems = ((Stream<?>) nestedStream).toList();
        assertThat(nestedItems).hasSize(1);
        assertThat(nestedItems.get(0)).isSameAs(listed);
        assertThat(container.iterator.next()).isSameAs(nested);
        assertThat(container.iterator.hasNext()).isFalse();
        assertThat(listed.getUrl()).isEqualTo("https://images.test/images/listed.png");
        assertThat(nested.getUrl()).isEqualTo("https://images.test/images/nested.png");
    }

    @Test
    void leavesPrimitiveArraysUntouched() {
        byte[] bytes = new byte[]{1, 2, 3};

        Object result = beforeBodyWrite(bytes);

        assertThat(result).isSameAs(bytes);
    }

    @Test
    void leavesEnumFieldsUntouched() {
        ImageVo view = new ImageVo();
        view.setStatus(ImageStatus.READY);

        processor.process(view);

        assertThat(view.getStatus()).isEqualTo(ImageStatus.READY);
    }

    @Test
    void leavesAUrlNullWhenTheObjectKeyIsNull() {
        AnnotatedView view = new AnnotatedView(null);
        view.setUrl("https://stale.test/image.png");

        processor.process(view);

        assertThat(view.getUrl()).isNull();
    }

    @Test
    void rejectsAnAnnotationThatNamesAMissingSourceField() {
        assertThatThrownBy(() -> processor.process(new InvalidView()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source field 'missingObjectKey' does not exist");
    }

    private Object beforeBodyWrite(Object body) {
        return processor.beforeBodyWrite(
                body,
                mock(MethodParameter.class),
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                mock(ServerHttpRequest.class),
                mock(ServerHttpResponse.class));
    }

    private static final class FieldOnlyContainer {
        public AnnotatedView view;

        private FieldOnlyContainer(AnnotatedView view) {
            this.view = view;
        }

        public AnnotatedView getView() {
            throw new AssertionError("Response processing must not invoke application getters");
        }
    }

    private static final class FinalIteratorContainer {
        private final Iterator<AnnotatedView> iterator;

        private FinalIteratorContainer(Iterator<AnnotatedView> iterator) {
            this.iterator = iterator;
        }
    }

    private static final class AnnotatedView {
        private String objectKey;
        @ObjectKeyToUrl("objectKey")
        private String url;

        private AnnotatedView(String objectKey) {
            this.objectKey = objectKey;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    private static final class InvalidView {
        @ObjectKeyToUrl("missingObjectKey")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
