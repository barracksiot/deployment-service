/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.deploymentservice.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.deploymentservice.model.DeviceRequest;
import io.barracks.deploymentservice.model.ResolvedPackages;
import io.barracks.deploymentservice.rest.PackageResource;
import io.barracks.deploymentservice.utils.ResolvedPackagesUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileCopyUtils;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PackageResource.class)
@AutoConfigureRestDocs("build/generated-snippets/deployment/packages")
public class PackageResourceConfigurationTest {
    private static final Endpoint RESOLVE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/packages/resolve");
    private static final String baseUrl = "https://not.barracks.io/";
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private PackageResource resource;

    @Value("classpath:io/barracks/deploymentservice/model/json/deviceRequest.json")
    private Resource request;

    @Test
    public void documentGetVersionsForRequest() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_ENDPOINT;
        final DeviceRequest expectedRequest = mapper.readValue(request.getInputStream(), DeviceRequest.class);
        final ResolvedPackages response = ResolvedPackagesUtils.getResolvedPackages();
        doReturn(response).when(resource).resolvePackages(expectedRequest);

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders
                .request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI()
                )
                .accept(MediaType.APPLICATION_JSON)
                .content(FileCopyUtils.copyToByteArray(request.getInputStream()))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(resource).resolvePackages(expectedRequest);
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(response)))
                .andDo(document(
                        "resolve",
                        requestFields(
                                fieldWithPath("userId").description("The user's ID"),
                                fieldWithPath("unitId").description("The device's ID").optional(),
                                fieldWithPath("additionalProperties").description("Request's additional properties").optional(),
                                fieldWithPath("packages").description("The list of packages used by the device").optional(),
                                fieldWithPath("packages[].reference").description("The package's reference").optional(),
                                fieldWithPath("packages[].version").description("The package's version currently used by the device")
                        ),
                        responseFields(
                                fieldWithPath("present").description("Currently allowed packages for the device"),
                                fieldWithPath("present[].reference").description("The package's reference"),
                                fieldWithPath("present[].version").description("The package's version (if available)"),
                                fieldWithPath("absent").description("Currently denied package for the device"),
                                fieldWithPath("present[].reference").description("The package's reference")
                        )
                ));
    }

    @Test
    public void postRequest_shouldCallResourceWithRequest_andReturnResults() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_ENDPOINT;
        final DeviceRequest expectedRequest = mapper.readValue(request.getInputStream(), DeviceRequest.class);
        final ResolvedPackages response = ResolvedPackagesUtils.getResolvedPackages();
        doReturn(response).when(resource).resolvePackages(expectedRequest);

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI()
                )
                .content(FileCopyUtils.copyToByteArray(request.getInputStream()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verify(resource).resolvePackages(expectedRequest);
        result.andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(response)));
    }

    @Test
    public void postRequest_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_ENDPOINT;

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI()
                )
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void postRequest_whenInvalidRequest_shouldReturnUnProcessableEntity() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_ENDPOINT;

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders
                .request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).getURI()
                )
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        verifyZeroInteractions(resource);
        result.andExpect(status().isUnprocessableEntity());
    }
}
