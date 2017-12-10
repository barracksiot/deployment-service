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
import com.google.common.collect.Lists;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@PersistenceConstructor}))
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "deploymentPlans")
@CompoundIndex(name = "packageRef_userId_createdAt", def = "{ 'packageRef': 1, 'userId': 1, 'created': -1 }")
public class DeploymentPlan {

    @Id
    @JsonIgnore
    private final String id;

    @JsonIgnore
    private final String userId;

    @NotBlank
    private final String packageRef;

    @JsonIgnore
    private final DeploymentConditions deploymentConditions;

    @JsonProperty("rules")
    @Singular
    private final List<DeploymentRule> deploymentRules;

    @JsonIgnore
    @CreatedDate
    private final Date created;

    @JsonCreator
    public static DeploymentPlan fromJson(
            @JsonProperty("allow") DeploymentCondition allow,
            @JsonProperty("deny") DeploymentCondition deny,
            @JsonProperty("rules") List<DeploymentRule> rules
    ) {
        final DeploymentPlanBuilder deploymentPlanBuilder = DeploymentPlan.builder()
                .deploymentRules(Optional.ofNullable(rules).map(ArrayList::new).orElse(new ArrayList<>()));
        if (allow != null || deny != null) {
            deploymentPlanBuilder.deploymentConditions(
                    DeploymentConditions.builder().allowCondition(allow).denyCondition(deny).build()
            );
        }
        return deploymentPlanBuilder.build();
    }

    public Optional<Date> getCreated() {
        return Optional.ofNullable(created).map(e -> new Date(e.getTime()));
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

    public List<DeploymentRule> getDeploymentRules() {
        return Optional.ofNullable(deploymentRules).map(ArrayList::new).orElse(new ArrayList<>());
    }

    public List<String> extractFilters() {
        final List<String> filters = Lists.newArrayList();
        filters.addAll(
                getDeploymentConditions()
                        .map(DeploymentConditions::extractFilters)
                        .orElse(Collections.emptyList())
        );

        filters.addAll(
                getDeploymentRules().stream()
                        .map(rule ->
                                rule.getDeploymentConditions()
                                        .map(DeploymentConditions::extractFilters)
                                        .orElse(Collections.emptyList())
                        )
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
        );
        return filters;
    }

    public List<String> extractVersions() {
        return getDeploymentRules().stream()
                .map(DeploymentRule::getVersionId)
                .collect(Collectors.toList());
    }

}
