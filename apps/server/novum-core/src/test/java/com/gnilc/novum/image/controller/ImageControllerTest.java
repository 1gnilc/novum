package com.gnilc.novum.image.controller;

import com.gnilc.common.utils.PageResult;
import com.gnilc.common.utils.PageParams;
import com.gnilc.novum.image.entity.vo.ImagePresignVo;
import com.gnilc.novum.image.entity.vo.ImageVo;
import com.gnilc.novum.image.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {
    private final ImageService service = mock(ImageService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ImageController(service)).build();
    }

    @Test
    void uploadRoutesPreserveTheDirectUploadContract() throws Exception {
        ImagePresignVo presign = new ImagePresignVo();
        presign.setObjectKey("images/2026/08/05/photo.webp");
        presign.setUploadUrl("https://upload.example.test/signed");
        presign.setMethod("PUT");
        presign.setHeaders(Map.of("Content-Type", "image/webp"));
        presign.setExpiresAt(Instant.parse("2026-08-05T04:40:00Z"));
        when(service.presign("image/webp", 2048L)).thenReturn(presign);

        ImageVo ready = new ImageVo();
        ready.setObjectKey(presign.getObjectKey());
        ready.setUrl("https://images.example.test/" + presign.getObjectKey());
        when(service.finalize("images/2026/08/05/photo.webp")).thenReturn(ready);

        mvc.perform(post("/image/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/webp\",\"contentLength\":2048}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectKey").value(presign.getObjectKey()))
                .andExpect(jsonPath("$.data.method").value("PUT"))
                .andExpect(jsonPath("$.data.headers.Content-Type").value("image/webp"))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-05T04:40:00Z"));

        mvc.perform(post("/image/finalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"images/2026/08/05/photo.webp\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url")
                        .value("https://images.example.test/images/2026/08/05/photo.webp"));
    }

    @Test
    void managementRoutesDelegatePageAndDelete() throws Exception {
        when(service.getImagePage(any(PageParams.class))).thenReturn(new PageResult<>());

        mvc.perform(post("/image/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPage\":2,\"pageSize\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(post("/image/remove/42"))
                .andExpect(status().isOk());

        verify(service).getImagePage(argThat(params ->
                params.getCurrentPage() == 2L && params.getPageSize() == 20L));
        verify(service).removeImage(42L);
    }

    @Test
    void jsonRequestBodiesAreRequired() throws Exception {
        mvc.perform(post("/image/presign").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/image/finalize").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/image/page").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

}
