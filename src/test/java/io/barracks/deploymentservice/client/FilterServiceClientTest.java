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

package io.barracks.deploymentservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.deploymentservice.client.exception.FilterServiceClientException;
import io.barracks.deploymentservice.model.DeviceRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.barracks.deploymentservice.client.FilterServiceClient.GET_FILTER_BY_NAME_ENDPOINT;
import static io.barracks.deploymentservice.client.FilterServiceClient.MATCH_DEVICE_EVENT_ENDPOINT;
import static io.barracks.deploymentservice.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(FilterServiceClient.class)
public class FilterServiceClientTest {
    @Value("${io.barracks.deviceservice.base_url}")
    private String baseUrl;
    @Autowired
    private FilterServiceClient filterServiceClient;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void filterExists_shouldReturnTrue_WhenServerReturn200Code() {
        // Given
        final Endpoint endpoint = GET_FILTER_BY_NAME_ENDPOINT;
        final String userId = "My user id";
        final String filterName = "The filter name";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withStatus(HttpStatus.OK));

        // When
        final boolean result = filterServiceClient.filterExists(userId, filterName);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void filterExists_shouldReturnFalse_WhenServerReturn404Code() {
        // Given
        final Endpoint endpoint = GET_FILTER_BY_NAME_ENDPOINT;
        final String userId = "My user id";
        final String filterName = "The filter name";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When
        final boolean result = filterServiceClient.filterExists(userId, filterName);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void filterExists_shouldThrowAnException_WhenServerReturnOtherCodes() {
        // Given
        final Endpoint endpoint = GET_FILTER_BY_NAME_ENDPOINT;
        final String userId = "My user id";
        final String filterName = "The filter name";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // When / Then
        assertThatExceptionOfType(FilterServiceClientException.class)
                .isThrownBy(() -> filterServiceClient.filterExists(userId, filterName));
        mockServer.verify();
    }

    @Test
    public void isRequestMatchingFilters_shouldReturnTrue_whenStatusOk() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_DEVICE_EVENT_ENDPOINT;
        final DeviceRequest request = getDeviceRequest();
        final List<String> filters = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.put("filter", filters);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).queryParams(query).getURI(request.getUserId(), request.getUnitId())))
                .andExpect(content().string(objectMapper.writeValueAsString(request)))
                .andRespond(withSuccess());

        // When
        final boolean result = filterServiceClient.isRequestMatchingFilters(request, filters);

        // Then
        mockServer.verify();
        assertThat(result).isTrue();
    }

    @Test
    public void isRequestMatchingFilters_shouldReturnFalse_whenStatusNotFound() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_DEVICE_EVENT_ENDPOINT;
        final DeviceRequest request = getDeviceRequest();
        final List<String> filters = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.put("filter", filters);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).queryParams(query).getURI(request.getUserId(), request.getUnitId())))
                .andExpect(content().string(objectMapper.writeValueAsString(request)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When
        final boolean result = filterServiceClient.isRequestMatchingFilters(request, filters);

        // Then
        mockServer.verify();
        assertThat(result).isFalse();
    }

    @Test
    public void isRequestMatchingFilters_shouldThrowException_whenServerError() throws Exception {
        // Given
        final Endpoint endpoint = MATCH_DEVICE_EVENT_ENDPOINT;
        final DeviceRequest request = getDeviceRequest();
        final List<String> filters = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.put("filter", filters);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).queryParams(query).getURI(request.getUserId(), request.getUnitId())))
                .andExpect(content().string(objectMapper.writeValueAsString(request)))
                .andRespond(withServerError());

        // When
        assertThatExceptionOfType(FilterServiceClientException.class)
                .isThrownBy(() -> filterServiceClient.isRequestMatchingFilters(request, filters));
        mockServer.verify();
    }
}