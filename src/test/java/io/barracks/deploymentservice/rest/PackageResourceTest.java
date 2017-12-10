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

package io.barracks.deploymentservice.rest;

import io.barracks.deploymentservice.manager.DeploymentPlanManager;
import io.barracks.deploymentservice.model.DeviceRequest;
import io.barracks.deploymentservice.model.ResolvedPackages;
import io.barracks.deploymentservice.utils.ResolvedPackagesUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.barracks.deploymentservice.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PackageResourceTest {

    @Mock
    private DeploymentPlanManager manager;

    @InjectMocks
    private PackageResource resource;

    @Test
    public void resolveComponents_shouldPassTheRequestToTheManager() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final ResolvedPackages expected = ResolvedPackagesUtils.getResolvedPackages();
        doReturn(expected).when(manager).resolvePackagesForDeviceRequest(request);

        // When
        final ResolvedPackages result = resource.resolvePackages(request);

        // Then
        verify(manager).resolvePackagesForDeviceRequest(request);
        assertThat(result).isEqualTo(expected);
    }
}
