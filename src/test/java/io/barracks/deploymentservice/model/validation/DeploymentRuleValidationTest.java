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

package io.barracks.deploymentservice.model.validation;

import io.barracks.deploymentservice.model.DeploymentConditions;
import io.barracks.deploymentservice.model.DeploymentPlan;
import io.barracks.deploymentservice.model.DeploymentRule;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentRuleValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void deploymentConditions_shouldNotBeNull() {
        // Given
        final DeploymentRule deploymentRule = DeploymentRule.builder().versionId("v0.0.1").build();

        // When
        final Set<ConstraintViolation<DeploymentRule>> violations = validator.validate(deploymentRule);

        // Then
        assertThat(violations).hasSize(1);
        final Path path = violations.iterator().next().getPropertyPath();
        assertThat(path).isEqualTo(PathImpl.createPathFromString("deploymentConditions"));
    }

    @Test
    public void versionId_shouldNotBeBlank() {
        // Given
        final DeploymentRule deploymentRule = DeploymentRule.builder().versionId("").deploymentConditions(
                DeploymentConditions.builder().build()
        ).build();

        // When
        final Set<ConstraintViolation<DeploymentRule>> violations = validator.validate(deploymentRule);

        // Then
        assertThat(violations).hasSize(1);
        final Path path = violations.iterator().next().getPropertyPath();
        assertThat(path).isEqualTo(PathImpl.createPathFromString("versionId"));
    }

    @Test
    public void versionId_shouldNotBeNull() {
        // Given
        final DeploymentRule deploymentRule = DeploymentRule.builder().deploymentConditions(
                DeploymentConditions.builder().build()
        ).build();

        // When
        final Set<ConstraintViolation<DeploymentRule>> violations = validator.validate(deploymentRule);

        // Then
        assertThat(violations).hasSize(1);
        final Path path = violations.iterator().next().getPropertyPath();
        assertThat(path).isEqualTo(PathImpl.createPathFromString("versionId"));
    }

}
