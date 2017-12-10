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

import io.barracks.commons.util.Endpoint;
import io.barracks.deploymentservice.client.exception.FilterServiceClientException;
import io.barracks.deploymentservice.model.DeviceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class FilterServiceClient {

    static final Endpoint GET_FILTER_BY_NAME_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters/{name}");
    static final Endpoint MATCH_DEVICE_EVENT_ENDPOINT = Endpoint.from(HttpMethod.POST, "/owners/{userId}/devices/{unitId}/match");

    private String baseUrl;
    private RestTemplate restTemplate;

    public FilterServiceClient(
            @Value("${io.barracks.deviceservice.base_url}") String baseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public boolean filterExists(String userId, String filterName) {
        try {
            final ResponseEntity<Void> response = restTemplate.exchange(
                    GET_FILTER_BY_NAME_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, filterName),
                    Void.class
            );
            return HttpStatus.OK.equals(response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                return false;
            }
            throw new FilterServiceClientException(e);
        }
    }

    public boolean isRequestMatchingFilters(DeviceRequest request, List<String> filters) {
        try {
            final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.put("filter", filters);
            final ResponseEntity<Void> response = restTemplate.exchange(
                    MATCH_DEVICE_EVENT_ENDPOINT.withBase(baseUrl).body(request).queryParams(queryParams).getRequestEntity(request.getUserId(), request.getUnitId()),
                    Void.class
            );
            return HttpStatus.OK.equals(response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                return false;
            }
            throw new FilterServiceClientException(e);
        }
    }

}
