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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@RestClientTest(ComponentServiceClient.class)
public class ComponentServiceClientTest {
    @Value("${io.barracks.componentservice.base_url}")
    private String baseUrl;
    @Autowired
    private ComponentServiceClient componentServiceClient;
    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    public void versionExists_shouldReturnTrue_whenServiceReturn200() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSION_ENDPOINT;
        final String userId = "My user id";
        final String packageRef = "io.barracks.app1";
        final String versionId = "v0.0.1";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, versionId)))
                .andRespond(withStatus(HttpStatus.OK));

        // When
        final boolean result = componentServiceClient.versionExists(userId, packageRef, versionId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void versionExists_shouldReturnFalse_whenServiceReturn404() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSION_ENDPOINT;
        final String userId = "My user id";
        final String packageRef = "io.barracks.app1";
        final String versionId = "v0.0.1";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, versionId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When
        final boolean result = componentServiceClient.versionExists(userId, packageRef, versionId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void versionExists_shouldThrowAnException_whenServiceReturnOtherCodes() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_VERSION_ENDPOINT;
        final String userId = "My user id";
        final String packageRef = "io.barracks.app1";
        final String versionId = "v0.0.1";
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef, versionId)))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // When / Then
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.versionExists(userId, packageRef, versionId));
        mockServer.verify();
    }

    @Test
    public void componentExists_shouldReturnTrue_whenServiceReturns200() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_PACKAGE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef)))
                .andRespond(withStatus(HttpStatus.OK));

        // When
        final boolean result = componentServiceClient.packageExists(userId, packageRef);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void componentExists_shouldReturnFalse_whenServiceReturns404() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_PACKAGE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When
        final boolean result = componentServiceClient.packageExists(userId, packageRef);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(false);
    }

    @Test
    public void componentExists_shouldThrowException_whenServiceReturnsOtherCodes() {
        // Given
        final Endpoint endpoint = ComponentServiceClient.GET_PACKAGE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, packageRef)))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // When / Then
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.packageExists(userId, packageRef));
        mockServer.verify();
    }
}