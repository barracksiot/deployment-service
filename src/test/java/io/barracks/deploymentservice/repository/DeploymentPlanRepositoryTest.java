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

package io.barracks.deploymentservice.repository;

import io.barracks.deploymentservice.model.DeploymentPlan;
import io.barracks.deploymentservice.utils.DeploymentPlanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataMongoTest(
        includeFilters = {@ComponentScan.Filter(classes = EnableMongoAuditing.class)}
)
@EnableMongoAuditing
public class DeploymentPlanRepositoryTest {

    private static final String PACKAGE_REF_KEY = "packageRef";

    @Autowired
    private DeploymentPlanRepository deploymentPlanRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static DeploymentPlan buildUnsavedDeploymentPlan() {
        return buildUnsavedDeploymentPlan(UUID.randomUUID().toString());
    }

    private static DeploymentPlan buildUnsavedDeploymentPlan(String userId) {
        return buildUnsavedDeploymentPlan(userId, UUID.randomUUID().toString());
    }

    private static DeploymentPlan buildUnsavedDeploymentPlan(String userId, String packageRef) {
        return DeploymentPlanUtils.getDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .id(null)
                .created(null)
                .build();
    }

    @Test
    public void insert_shouldInsert_andResultWithIdAndDate() {
        // Given
        final DeploymentPlan plan = buildUnsavedDeploymentPlan();

        // When
        final DeploymentPlan result = deploymentPlanRepository.insert(plan.toBuilder().build());

        // Then
        final DeploymentPlan expected = plan.toBuilder()
                .id(result.getId())
                .created(result.getCreated().orElseThrow(() -> new RuntimeException("Failed to get date")))
                .build();
        final DeploymentPlan document = mongoTemplate.findOne(Query.query(Criteria.where(PACKAGE_REF_KEY).is(result.getPackageRef())), DeploymentPlan.class);
        assertThat(result).isNotNull().isEqualTo(document).isEqualTo(expected).hasNoNullFieldsOrProperties();
    }

    @Test
    public void insert_shouldInsert_whenDeploymentPlanDoesNotContainAnyRules() {
        // Given
        final DeploymentPlan plan = buildUnsavedDeploymentPlan().toBuilder()
                .clearDeploymentRules()
                .deploymentConditions(null)
                .build();

        // When
        final DeploymentPlan result = deploymentPlanRepository.insert(plan.toBuilder().build());

        // Then
        final DeploymentPlan expected = plan.toBuilder()
                .id(result.getId())
                .created(result.getCreated().orElseThrow(() -> new RuntimeException("Failed to get date")))
                .build();
        final DeploymentPlan document = mongoTemplate.findOne(Query.query(Criteria.where(PACKAGE_REF_KEY).is(result.getPackageRef())), DeploymentPlan.class);
        assertThat(result).isNotNull().isEqualTo(document).isEqualTo(expected);
    }

    @Test
    public void insert_shouldInsert_whenDeploymentPlanContainsConditions() {
        // Given
        final DeploymentPlan plan = buildUnsavedDeploymentPlan().toBuilder()
                .clearDeploymentRules()
                .build();

        // When
        final DeploymentPlan result = deploymentPlanRepository.insert(plan.toBuilder().build());

        // Then
        final DeploymentPlan expected = plan.toBuilder()
                .id(result.getId())
                .created(result.getCreated().orElseThrow(() -> new RuntimeException("Failed to get date")))
                .build();
        final DeploymentPlan document = mongoTemplate.findOne(Query.query(Criteria.where(PACKAGE_REF_KEY).is(result.getPackageRef())), DeploymentPlan.class);
        assertThat(document).isNotNull().isEqualTo(result).isEqualTo(expected);
    }

    @Test
    public void insert_shouldInsert_whenDeploymentPlanContainsRules() {
        // Given
        final DeploymentPlan plan = buildUnsavedDeploymentPlan().toBuilder()
                .deploymentConditions(null)
                .build();

        // When
        final DeploymentPlan result = deploymentPlanRepository.insert(plan.toBuilder().build());

        // Then
        final DeploymentPlan expected = plan.toBuilder()
                .id(result.getId())
                .created(result.getCreated().orElseThrow(() -> new RuntimeException("Failed to get date")))
                .build();
        final DeploymentPlan document = mongoTemplate.findOne(Query.query(Criteria.where(PACKAGE_REF_KEY).is(result.getPackageRef())), DeploymentPlan.class);
        assertThat(result).isNotNull();
        assertThat(document).isNotNull().isEqualTo(result).isEqualTo(expected);
    }

    @Test
    public void findByUserId_whenNoPlan_shouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();

        // When
        final List<DeploymentPlan> result = deploymentPlanRepository.findByUserId(userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void findByUserId_whenPlans_shouldReturnList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<DeploymentPlan> plans = Arrays.asList(
                buildUnsavedDeploymentPlan(userId),
                buildUnsavedDeploymentPlan(userId)
        );
        deploymentPlanRepository.save(plans);

        // When
        final List<DeploymentPlan> result = deploymentPlanRepository.findByUserId(userId);

        // Then
        assertThat(result).containsOnlyElementsOf(plans);
    }

    @Test
    public void findByUserId_whenMultiplePlansVersions_shouldReturnOnlyLatest() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<String> packageRefs = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        final List<DeploymentPlan> plans = Arrays.asList(
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(1)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(1))
        );
        deploymentPlanRepository.save(plans);

        // When
        final List<DeploymentPlan> result = deploymentPlanRepository.findByUserId(userId);

        // Then
        assertThat(result).containsOnlyElementsOf(Arrays.asList(plans.get(2), plans.get(4)));
    }

    @Test
    public void findByUserId_whenMultiplePlansVersions_shouldReturnOnlyUsersPlans() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final List<String> packageRefs = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        final List<DeploymentPlan> plans = Arrays.asList(
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(0)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(1)),
                buildUnsavedDeploymentPlan(userId, packageRefs.get(1)),

                buildUnsavedDeploymentPlan(UUID.randomUUID().toString(), packageRefs.get(0)),
                buildUnsavedDeploymentPlan(UUID.randomUUID().toString(), packageRefs.get(1)),
                buildUnsavedDeploymentPlan()
        );
        deploymentPlanRepository.save(plans);

        // When
        final List<DeploymentPlan> result = deploymentPlanRepository.findByUserId(userId);

        // Then
        assertThat(result).containsOnlyElementsOf(Arrays.asList(plans.get(2), plans.get(4)));
    }

    @Test
    public void getDeployedVersions_whenNoDeploymentPlan_shouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();

        // When
        final List<String> result = deploymentPlanRepository.getDeployedVersions(userId, packageRef);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getDeployedVersions_whenDeploymentPlanWithoutVersion_shouldReturnEmptyList() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = buildUnsavedDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .clearDeploymentRules()
                .build();

        deploymentPlanRepository.save(deploymentPlan);

        // When
        final List<String> result = deploymentPlanRepository.getDeployedVersions(userId, packageRef);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getDeployedVersions_whenDeploymentPlanWithVersions_shouldReturnDeployedVersions() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = buildUnsavedDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .build();

        deploymentPlanRepository.save(deploymentPlan);

        // When
        final List<String> result = deploymentPlanRepository.getDeployedVersions(userId, packageRef);

        // Then
        assertThat(result).containsAll(deploymentPlan.extractVersions());
    }

    @Test
    public void getDeployedVersions_whenMultipleDeploymentPlanWithVersions_shouldReturnDeployedVersions() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = buildUnsavedDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .build();
        final DeploymentPlan deploymentPlan2 = buildUnsavedDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .build();
        final DeploymentPlan deploymentPlan3 = buildUnsavedDeploymentPlan().toBuilder()
                .userId(userId)
                .packageRef(packageRef)
                .build();
        final List<DeploymentPlan> plans = Arrays.asList(deploymentPlan, deploymentPlan2, deploymentPlan3);
        deploymentPlanRepository.save(plans);

        // When
        final List<String> result = deploymentPlanRepository.getDeployedVersions(userId, packageRef);

        // Then
        assertThat(result).containsAll(deploymentPlan.extractVersions());
        assertThat(result).containsAll(deploymentPlan2.extractVersions());
        assertThat(result).containsAll(deploymentPlan3.extractVersions());
    }

    @Test
    public void getDeploymentPlansByFilterName_whenPlansUseFilter_shouldReturnPlans() {
        // Given
        final Pageable pageable = new PageRequest(0, 10);
        final String userId = UUID.randomUUID().toString();
        final DeploymentPlan toFind = buildUnsavedDeploymentPlan(userId);

        final List<DeploymentPlan> plans = Arrays.asList(
                toFind,
                buildUnsavedDeploymentPlan(UUID.randomUUID().toString()),
                buildUnsavedDeploymentPlan(UUID.randomUUID().toString())
        );
        deploymentPlanRepository.save(plans);

        // When
        List<DeploymentPlan> result = new ArrayList<>();
        toFind.extractFilters().stream()
                .map(name -> deploymentPlanRepository.findByFilterNameAndUserId(userId, name, pageable))
                .map(Slice::getContent)
                .forEach(result::addAll);

        // Then
        assertThat(result).hasSize(toFind.extractFilters().size()).containsOnly(toFind);
    }

    @Test
    public void getDeploymentPlansByFilterName_whenManyPlansUseFilter_shouldReturnPlans() {

        // Given
        final int pageSize = 5;
        final Pageable pageable = new PageRequest(0, pageSize);
        final String userId = UUID.randomUUID().toString();
        final DeploymentPlan toFind = buildUnsavedDeploymentPlan(userId);

        final List<DeploymentPlan> plans = IntStream.range(0, 15)
                .mapToObj(
                        nbr -> toFind.toBuilder().packageRef(UUID.randomUUID().toString()).build()
                ).collect(Collectors.toList());

        deploymentPlanRepository.save(plans);

        // When
        Page<DeploymentPlan> result = deploymentPlanRepository.findByFilterNameAndUserId(userId, toFind.extractFilters().get(0), pageable);

        // Then
        assertThat(result).hasSize(pageSize).isSubsetOf(plans);
        assertThat(result.getTotalElements()).isEqualTo(plans.size());
    }

    @Test
    public void getDeploymentPlansByFilterName_whenPlansHasNoFilter_shouldReturnEmptyPage() {
        // Given
        final Pageable pageable = new PageRequest(0, 5);
        final DeploymentPlan aPlan = buildUnsavedDeploymentPlan();
        final String userId = aPlan.getUserId();

        final List<DeploymentPlan> plans = Arrays.asList(
                aPlan,
                aPlan.toBuilder().id(UUID.randomUUID().toString()).build()
        );

        deploymentPlanRepository.save(plans);

        // When
        Page<DeploymentPlan> result = deploymentPlanRepository.findByFilterNameAndUserId(userId, "UnusedName", pageable);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveDeploymentPlan_whenPlanDoesNotExist_shouldReturnAnEmptyOptional() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();

        // When
        final Optional<DeploymentPlan> result = deploymentPlanRepository.getActiveDeploymentPlan(userId, packageRef);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveDeploymentPlan_whenPlanExists_shouldReturnThePlan() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan plan = buildUnsavedDeploymentPlan(userId, packageRef);
        deploymentPlanRepository.save(plan);

        // When
        final Optional<DeploymentPlan> result = deploymentPlanRepository.getActiveDeploymentPlan(userId, packageRef);

        // Then
        assertThat(result)
                .isPresent()
                .contains(plan);
    }

    @Test
    public void getActiveDeploymentPlan_whenPlanHasMoreThanOneVersion_shouldReturnTheLatest() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan oldVersion = buildUnsavedDeploymentPlan(userId, packageRef);
        deploymentPlanRepository.save(oldVersion);
        final DeploymentPlan newVersion = buildUnsavedDeploymentPlan(userId, packageRef);
        deploymentPlanRepository.save(newVersion);

        // When
        final Optional<DeploymentPlan> result = deploymentPlanRepository.getActiveDeploymentPlan(userId, packageRef);

        // Then
        assertThat(result)
                .isPresent()
                .contains(newVersion);
    }

}