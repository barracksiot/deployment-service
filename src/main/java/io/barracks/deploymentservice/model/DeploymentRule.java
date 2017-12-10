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

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.PersistenceConstructor;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@PersistenceConstructor}))
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@ToString
public class DeploymentRule {

    @NotBlank
    @JsonProperty("version")
    private final String versionId;

    @NotNull
    @JsonIgnore
    private final DeploymentConditions deploymentConditions;

    @JsonCreator
    public static DeploymentRule fromJson(
            @JsonProperty("version") String versionId,
            @JsonProperty("allow") DeploymentCondition allow,
            @JsonProperty("deny") DeploymentCondition deny
    ) {
        final DeploymentRule.DeploymentRuleBuilder deploymentPlanBuilder = DeploymentRule.builder()
                .versionId(versionId);
        if (allow != null || deny != null) {
            deploymentPlanBuilder.deploymentConditions(
                    DeploymentConditions.builder().allowCondition(allow).denyCondition(deny).build()
            );
        }
        return deploymentPlanBuilder.build();
    }

    @JsonProperty("allow")
    public Optional<DeploymentCondition> getAllow() {
        return Optional.ofNullable(deploymentConditions)
                .flatMap(DeploymentConditions::getAllowCondition);
    }

    @JsonProperty("deny")
    public Optional<DeploymentCondition> getDeny() {
        return Optional.ofNullable(deploymentConditions)
                .flatMap(DeploymentConditions::getDenyCondition);
    }

    public Optional<DeploymentConditions> getDeploymentConditions() {
        return Optional.ofNullable(deploymentConditions);
    }

}
