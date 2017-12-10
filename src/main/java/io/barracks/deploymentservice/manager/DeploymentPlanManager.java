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

package io.barracks.deploymentservice.manager;

import io.barracks.deploymentservice.client.ComponentServiceClient;
import io.barracks.deploymentservice.client.FilterServiceClient;
import io.barracks.deploymentservice.exception.InvalidFiltersException;
import io.barracks.deploymentservice.exception.InvalidPackageException;
import io.barracks.deploymentservice.exception.InvalidVersionsException;
import io.barracks.deploymentservice.exception.UnknownDeploymentPlanException;
import io.barracks.deploymentservice.model.*;
import io.barracks.deploymentservice.model.Package;
import io.barracks.deploymentservice.repository.DeploymentPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeploymentPlanManager {

    private final DeploymentPlanRepository deploymentPlanRepository;
    private final FilterServiceClient filterServiceClient;
    private final ComponentServiceClient componentServiceClient;

    public DeploymentPlanManager(
            DeploymentPlanRepository deploymentPlanRepository,
            FilterServiceClient filterServiceClient,
            ComponentServiceClient componentServiceClient
    ) {
        this.deploymentPlanRepository = deploymentPlanRepository;
        this.filterServiceClient = filterServiceClient;
        this.componentServiceClient = componentServiceClient;
    }

    public DeploymentPlan publishDeploymentPlan(DeploymentPlan deploymentPlan) {
        validateDeploymentPlanFilters(deploymentPlan);
        validateDeploymentPlanVersions(deploymentPlan);
        validateDeploymentPlanPackage(deploymentPlan);
        return deploymentPlanRepository.insert(deploymentPlan);
    }

    void validateDeploymentPlanFilters(DeploymentPlan deploymentPlan) {
        final List<String> filters = deploymentPlan.extractFilters();
        final List<String> invalidFilters = filters.parallelStream()
                .filter(name -> !filterServiceClient.filterExists(deploymentPlan.getUserId(), name))
                .collect(Collectors.toList());
        if (!invalidFilters.isEmpty()) {
            throw new InvalidFiltersException(
                    deploymentPlan.getUserId(),
                    deploymentPlan.getPackageRef(),
                    invalidFilters
            );
        }
    }

    void validateDeploymentPlanVersions(DeploymentPlan deploymentPlan) {
        final List<String> versions = deploymentPlan.extractVersions();
        final List<String> invalidVersions = versions.parallelStream()
                .filter(versionId ->
                        !componentServiceClient.versionExists(
                                deploymentPlan.getUserId(),
                                deploymentPlan.getPackageRef(),
                                versionId
                        )
                )
                .collect(Collectors.toList());
        if (!invalidVersions.isEmpty()) {
            throw new InvalidVersionsException(
                    deploymentPlan.getUserId(),
                    deploymentPlan.getPackageRef(),
                    invalidVersions
            );
        }
    }

    void validateDeploymentPlanPackage(DeploymentPlan deploymentPlan) {
        final String userId = deploymentPlan.getUserId();
        final String packageRef = deploymentPlan.getPackageRef();
        if (!componentServiceClient.packageExists(userId, packageRef)) {
            throw new InvalidPackageException(userId, packageRef);
        }
    }

    public DeploymentPlan getActiveDeploymentPlan(String userId, String reference) {
        return deploymentPlanRepository.getActiveDeploymentPlan(userId, reference)
                .orElseThrow(() -> new UnknownDeploymentPlanException(userId, reference));
    }

    public ResolvedPackages resolvePackagesForDeviceRequest(DeviceRequest request) {
        final ResolvedPackages.ResolvedPackagesBuilder builder = ResolvedPackages.builder();
        final List<DeploymentPlan> plans = deploymentPlanRepository.findByUserId(request.getUserId());
        plans.forEach(
                plan -> {
                    Optional<Package> pkg = getPackageForPlan(request, plan);
                    if (pkg.isPresent()) {
                        builder.present(pkg.get());
                    } else {
                        builder.absent(Package.builder().reference(plan.getPackageRef()).build());
                    }
                }
        );
        return builder.build();
    }

    Optional<Package> getPackageForPlan(DeviceRequest request, DeploymentPlan deploymentPlan) {
        return Optional.of(deploymentPlan)
                .filter(plan -> isPackageAvailable(request, plan))
                .map(plan -> getPackageVersion(request, deploymentPlan)
                        .map(version -> Package.builder().reference(deploymentPlan.getPackageRef()).version(version).build())
                        .orElse(Package.builder().reference(deploymentPlan.getPackageRef()).build())
                );
    }

    Optional<String> getPackageVersion(DeviceRequest request, DeploymentPlan plan) {
        return plan.getDeploymentRules().stream()
                .filter(rule -> isVersionAvailable(request, rule))
                .map(DeploymentRule::getVersionId)
                .findFirst();
    }

    boolean isPackageAvailable(DeviceRequest request, DeploymentPlan plan) {
        return plan.getAllow()
                .filter(allow -> !allow.getFilters().isEmpty())
                .map(condition -> filterServiceClient.isRequestMatchingFilters(request, condition.getFilters()))
                .orElse(true) &&
                plan.getDeny()
                        .filter(deny -> !deny.getFilters().isEmpty())
                        .map(condition -> !filterServiceClient.isRequestMatchingFilters(request, condition.getFilters()))
                        .orElse(true);
    }

    boolean isVersionAvailable(DeviceRequest request, DeploymentRule rule) {
        return rule.getAllow()
                .filter(allow -> !allow.getFilters().isEmpty())
                .map(allow -> filterServiceClient.isRequestMatchingFilters(request, allow.getFilters()))
                .orElse(true) &&
                rule.getDeny()
                        .filter(deny -> !deny.getFilters().isEmpty())
                        .map(deny -> !filterServiceClient.isRequestMatchingFilters(request, deny.getFilters()))
                        .orElse(true);
    }

    public Page<DeploymentPlan> getDeploymentPlansByFilterName(String filterName, String userId, Pageable pageable) {
        return deploymentPlanRepository.findByFilterNameAndUserId(userId, filterName, pageable);
    }

    public List<String> getDeployedVersions(String userId, String packageRef, boolean onlyActive) {
        if (onlyActive) {
            return deploymentPlanRepository.getActiveDeploymentPlan(userId, packageRef).map(DeploymentPlan::extractVersions).orElse(Collections.emptyList());
        } else {
            return deploymentPlanRepository.getDeployedVersions(userId, packageRef);
        }
    }
}
