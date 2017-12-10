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
import io.barracks.deploymentservice.client.exception.ComponentServiceClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class ComponentServiceClient {

    static final Endpoint GET_PACKAGE_ENDPOINT = Endpoint.from(
            HttpMethod.GET,
            "/owners/{userId}/packages/{packageRef}"
    );

    static final Endpoint GET_VERSION_ENDPOINT = Endpoint.from(
            HttpMethod.GET,
            "/owners/{userId}/packages/{packageRef}/versions/{versionId}"
    );

    private String baseUrl;
    private RestTemplate restTemplate;

    public ComponentServiceClient(
            @Value("${io.barracks.componentservice.base_url}") String baseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public boolean versionExists(String userId, String packageRef, String versionId) {
        try {
            final ResponseEntity<Void> response = restTemplate.exchange(
                    GET_VERSION_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, packageRef, versionId),
                    Void.class
            );
            return HttpStatus.OK.equals(response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                return false;
            }
            throw new ComponentServiceClientException(e);
        }
    }

    public boolean packageExists(String userId, String packageRef) {
        try {
            final ResponseEntity<Void> response = restTemplate.exchange(
                    GET_PACKAGE_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, packageRef),
                    Void.class
            );
            return HttpStatus.OK.equals(response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                return false;
            }
            throw new ComponentServiceClientException(e);
        }
    }
}
