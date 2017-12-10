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

package io.barracks.deploymentservice.model.json;

import io.barracks.deploymentservice.model.DeploymentPlan;
import io.barracks.deploymentservice.utils.DeploymentConditionsUtils;
import io.barracks.deploymentservice.utils.DeploymentPlanUtils;
import io.barracks.deploymentservice.utils.DeploymentRuleUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeploymentPlanJsonTest {

    @Value("classpath:/io/barracks/deploymentservice/model/json/deploymentPlanWithoutPackageConditions.json")
    private Resource deploymentPlanWithoutPackageConditions;

    @Value("classpath:/io/barracks/deploymentservice/model/json/deploymentPlanWithPackageConditions.json")
    private Resource deploymentPlanWithPackageConditions;

    @Value("classpath:/io/barracks/deploymentservice/model/json/deploymentPlanWithAllowAndWithoutRules-request.json")
    private Resource deploymentPlanWithAllowAndWithoutRulesRequest;

    @Value("classpath:/io/barracks/deploymentservice/model/json/deploymentPlanWithAllowAndWithoutRules-response.json")
    private Resource deploymentPlanWithAllowAndWithoutRulesResponse;

    @Autowired
    private JacksonTester<DeploymentPlan> jsonTester;


    @Test
    public void serializeJsonWithoutPackageCondition() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentRules(
                        DeploymentRuleUtils.buildDeploymentRules("v0.0.1", Arrays.asList("filter1", "filter2"))
                ).build();

        // When
        final JsonContent<DeploymentPlan> jsonContent = jsonTester.write(deploymentPlan);

        // Then
        assertThat(jsonContent).isEqualToJson(deploymentPlanWithoutPackageConditions);
    }

    @Test
    public void serializeJsonWithPackageCondition() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentConditions(
                        DeploymentConditionsUtils.buildDeploymentConditions(Arrays.asList("filterA", "filterB"))
                )
                .deploymentRules(
                        DeploymentRuleUtils.buildDeploymentRules("v0.0.1", Collections.singletonList("filter1"))
                ).build();

        // When
        final JsonContent<DeploymentPlan> jsonContent = jsonTester.write(deploymentPlan);

        // Then
        assertThat(jsonContent).isEqualToJson(deploymentPlanWithPackageConditions);
    }

    @Test
    public void deserializeJsonWithPackageCondition() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentConditions(
                        DeploymentConditionsUtils.buildDeploymentConditions(Arrays.asList("filterA", "filterB"))
                )
                .deploymentRules(
                        DeploymentRuleUtils.buildDeploymentRules("v0.0.1", Collections.singletonList("filter1"))
                ).build();

        // When
        final ObjectContent<DeploymentPlan> content = jsonTester.read(deploymentPlanWithPackageConditions);

        // Then
        assertThat(content.getObject()).isEqualTo(deploymentPlan);
    }

    @Test
    public void deserializeJsonWithoutPackageCondition() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentRules(
                        DeploymentRuleUtils.buildDeploymentRules("v0.0.1", Arrays.asList("filter1", "filter2"))
                ).build();

        // When
        final ObjectContent<DeploymentPlan> content = jsonTester.read(deploymentPlanWithoutPackageConditions);

        // Then
        assertThat(content.getObject()).isEqualTo(deploymentPlan);
    }

    @Test
    public void deserializeJsonWithAllowAndWithoutRules() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentConditions(
                        DeploymentConditionsUtils.buildDeploymentConditions(Arrays.asList("filterA", "filterB"))
                ).build();

        // When
        final ObjectContent<DeploymentPlan> content = jsonTester.read(deploymentPlanWithAllowAndWithoutRulesRequest);

        // Then
        assertThat(content.getObject()).isEqualTo(deploymentPlan);
    }

    @Test
    public void serializeJsonWithAllowAndWithoutRules() throws IOException {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlan.builder()
                .packageRef("io.barracks.app1")
                .deploymentConditions(
                        DeploymentConditionsUtils.buildDeploymentConditions(Arrays.asList("filterA", "filterB"))
                ).build();

        // When
        final JsonContent<DeploymentPlan> jsonContent = jsonTester.write(deploymentPlan);

        // Then
        assertThat(jsonContent).isEqualToJson(deploymentPlanWithAllowAndWithoutRulesResponse);
    }

    @Test
    public void serializeShouldIgnoreIdAndUserIdAndDate() throws Exception {
        // Given
        final DeploymentPlan plan = DeploymentPlanUtils.getDeploymentPlan();

        // When
        final JsonContent<DeploymentPlan> jsonContent = jsonTester.write(plan);

        // Then
        assertThat(jsonContent)
                .doesNotHaveJsonPathValue("id")
                .doesNotHaveJsonPathValue("userId")
                .doesNotHaveJsonPathValue("created");
    }

}
