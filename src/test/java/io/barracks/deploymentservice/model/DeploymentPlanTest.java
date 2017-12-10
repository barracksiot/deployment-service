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

package io.barracks.deploymentservice.model;

import io.barracks.deploymentservice.utils.DeploymentConditionUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeploymentPlanTest {

    @Test
    public void extractFilters_shouldExtractFiltersFromConditions() {
        // Given
        final List<String> filters = Arrays.asList("filter1", "filter2");
        final DeploymentConditions deploymentConditions = mock(DeploymentConditions.class);
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .deploymentConditions(deploymentConditions)
                .build();
        when(deploymentConditions.extractFilters()).thenReturn(filters);

        // When
        final List<String> results = deploymentPlan.extractFilters();

        // Then
        assertThat(results).containsAll(filters);
        verify(deploymentConditions).extractFilters();
    }

    @Test
    public void extractFilters_shouldExtractFiltersFromRules() {
        // Given
        final List<String> filters = Arrays.asList("filter1", "filter2");
        final DeploymentConditions deploymentConditions = mock(DeploymentConditions.class);
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .deploymentRule(
                        DeploymentRule.builder()
                                .versionId("v0.0.1")
                                .deploymentConditions(deploymentConditions)
                                .build()
                )
                .build();
        when(deploymentConditions.extractFilters()).thenReturn(filters);

        // When
        final List<String> results = deploymentPlan.extractFilters();

        // Then
        assertThat(results).containsAll(filters);
        verify(deploymentConditions).extractFilters();
    }

    @Test
    public void extractVersions_shouldExtractVersionsFromRules() {
        // Given
        final List<String> versions = Arrays.asList("v0.0.1", "v0.0.2");
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .deploymentRule(
                        DeploymentRule.builder()
                                .versionId(versions.get(0))
                                .build()
                )
                .deploymentRule(
                        DeploymentRule.builder()
                                .versionId(versions.get(1))
                                .build()
                )
                .build();

        // When
        final List<String> results = deploymentPlan.extractVersions();

        // Then
        assertThat(results).containsAll(versions);
    }

}