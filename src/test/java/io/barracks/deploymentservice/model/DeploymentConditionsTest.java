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

public class DeploymentConditionsTest {

    @Test
    public void extractFilters_shouldExtractFiltersFromAllowCondition() {
        // Given
        final List<String> allowedFilters = Arrays.asList("filter1", "filter2");
        final DeploymentConditions deploymentConditions = DeploymentConditions.builder()
                .allowCondition(
                        DeploymentConditionUtils.buildDeploymentCondition()
                                .toBuilder()
                                .clearFilters()
                                .filters(allowedFilters)
                                .build()
                )
                .build();

        // When
        final List<String> filters = deploymentConditions.extractFilters();

        // Then
        assertThat(filters).containsAll(allowedFilters);
    }

    @Test
    public void extractFilters_shouldExtractFiltersFromDenyCondition() {
        // Given
        final List<String> deniedFilters = Arrays.asList("filter1", "filter2");
        final DeploymentConditions deploymentConditions = DeploymentConditions.builder()
                .denyCondition(
                        DeploymentConditionUtils.buildDeploymentCondition()
                                .toBuilder()
                                .clearFilters()
                                .filters(deniedFilters)
                                .build()
                )
                .build();

        // When
        final List<String> filters = deploymentConditions.extractFilters();

        // Then
        assertThat(filters).containsAll(deniedFilters);
    }

}