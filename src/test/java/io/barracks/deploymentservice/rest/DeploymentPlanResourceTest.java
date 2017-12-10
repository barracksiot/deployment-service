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

import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.deploymentservice.manager.DeploymentPlanManager;
import io.barracks.deploymentservice.model.DeploymentPlan;
import io.barracks.deploymentservice.utils.DeploymentPlanUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import java.util.Collections;
import java.util.UUID;

import static io.barracks.deploymentservice.utils.DeploymentPlanUtils.getDeploymentPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentPlanResourceTest {

    @Mock
    private DeploymentPlanManager deploymentPlanManager;

    private DeploymentPlanResource deploymentPlanResource;

    private PagedResourcesAssembler<DeploymentPlan> assembler = PagedResourcesUtils.getPagedResourcesAssembler();

    @Before
    public void setup() {
        deploymentPlanResource = new DeploymentPlanResource(deploymentPlanManager, assembler);
    }

    @Test
    public void publishDeploymentPlan_shouldPassTheDeploymentPlanToTheManager() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = DeploymentPlanUtils.getDeploymentPlan().toBuilder().userId(null).build();
        final DeploymentPlan deploymentPlanWithUserId = deploymentPlan.toBuilder().userId(userId).build();
        final DeploymentPlan expected = getDeploymentPlan();
        when(deploymentPlanManager.publishDeploymentPlan(deploymentPlanWithUserId)).thenReturn(expected);

        // When
        final DeploymentPlan result = deploymentPlanResource.publishDeploymentPlan(deploymentPlan, userId);

        // Then
        verify(deploymentPlanManager).publishDeploymentPlan(deploymentPlanWithUserId);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getDeploymentPlanByFilterName_whenAllIsFine_shouldCallManagerSndReturnResult() {
        // Given
        final String filterName = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final DeploymentPlan plan1 = getDeploymentPlan().toBuilder().userId(userId).build();
        final DeploymentPlan plan2 = getDeploymentPlan().toBuilder().userId(userId).build();
        final Page<DeploymentPlan> page = new PageImpl<>(Lists.newArrayList(plan1, plan2));
        final PagedResources<Resource<DeploymentPlan>> expected = assembler.toResource(page);
        when(deploymentPlanManager.getDeploymentPlansByFilterName(filterName, userId, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<DeploymentPlan>> result = deploymentPlanResource.getDeploymentPlansByFilterName(filterName, userId, pageable);

        // Then
        verify(deploymentPlanManager).getDeploymentPlansByFilterName(filterName, userId, pageable);
        assertThat(result).isEqualTo(expected);
    }


    @Test
    public void getDeploymentPlanByFilterName_whenAllIsFineAndNoPlan_shouldCallManagerAndReturnEmptyPage() {
        // Given
        final String filterName = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final Page<DeploymentPlan> page = new PageImpl<>(Collections.emptyList());
        final PagedResources<Resource<DeploymentPlan>> expected = assembler.toResource(page);
        when(deploymentPlanManager.getDeploymentPlansByFilterName(filterName, userId, pageable)).thenReturn(page);

        // When
        final PagedResources<Resource<DeploymentPlan>> result = deploymentPlanResource.getDeploymentPlansByFilterName(filterName, userId, pageable);

        // Then
        verify(deploymentPlanManager).getDeploymentPlansByFilterName(filterName, userId, pageable);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getActiveDeploymentPlan_shouldCallManagerAndReturnTheResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        when(deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef)).thenReturn(deploymentPlan);

        // When
        final DeploymentPlan result = deploymentPlanResource.getActiveDeploymentPlan(userId, packageRef);

        // Then
        verify(deploymentPlanManager).getActiveDeploymentPlan(userId, packageRef);
        assertThat(result).isNotNull().isEqualTo(result);
    }
}