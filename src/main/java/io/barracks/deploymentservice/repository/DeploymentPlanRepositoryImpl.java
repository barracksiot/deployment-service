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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Slf4j
public class DeploymentPlanRepositoryImpl implements DeploymentPlanRepositoryCustom {

    public static final String PACKAGE_REF_KEY = "packageRef";
    public static final String USER_ID_KEY = "userId";
    public static final String PLAN_ALLOW_CONDITION_FILTER_KEY = "deploymentConditions.allowCondition.filters";
    public static final String PLAN_DENY_CONDITION_FILTER_KEY = "deploymentConditions.denyCondition.filters";
    public static final String PLAN_RULE_ALLOW_CONDITION_FILTER_KEY = "deploymentRules.deploymentConditions.allowCondition.filters";
    public static final String PLAN_RULE_DNY_CONDITION_FILTER_KEY = "deploymentRules.deploymentConditions.denyCondition.filters";
    public static final String PLAN_RULE_VERSION_KEY = "deploymentRules.versionId";
    public static final String CREATED_KEY = "created";
    private final MongoOperations operations;

    @Autowired
    public DeploymentPlanRepositoryImpl(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public List<DeploymentPlan> findByUserId(String userId) {
        final List<?> packages = operations.getCollection(operations.getCollectionName(DeploymentPlan.class))
                .distinct(PACKAGE_REF_KEY, query(where(USER_ID_KEY).is(userId)).getQueryObject());
        log.debug("Found " + packages + " for user " + userId);
        return packages.stream()
                .map(pkg -> getActiveDeploymentPlan(userId, pkg.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    @Override
    public Optional<DeploymentPlan> getActiveDeploymentPlan(String userId, String packageRef) {
        return operations.find(
                query(where(USER_ID_KEY).is(userId).and(PACKAGE_REF_KEY).is(packageRef)).with(new Sort(Sort.Direction.DESC, CREATED_KEY)).limit(1),
                DeploymentPlan.class
        ).stream().findFirst();
    }

    @Override
    public List<String> getDeployedVersions(String userId, String packageRef) {
        final Query query = query(where(USER_ID_KEY).is(userId).and(PACKAGE_REF_KEY).is(packageRef));
        final List<?> deployedVersions = operations.getCollection(operations.getCollectionName(DeploymentPlan.class))
                .distinct(PLAN_RULE_VERSION_KEY, query.getQueryObject());
        return deployedVersions
                .stream()
                .map(obj -> (String) obj)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DeploymentPlan> findByFilterNameAndUserId(String userId, String filterName, Pageable pageable) {

        final List<String> planIds = findByUserId(userId).stream().map(DeploymentPlan::getId).collect(toList());
        final Criteria criteria = new Criteria().andOperator(
                where("_id").in(planIds),
                new Criteria().orOperator(
                        where(PLAN_ALLOW_CONDITION_FILTER_KEY).is(filterName),
                        where(PLAN_DENY_CONDITION_FILTER_KEY).is(filterName),
                        where(PLAN_RULE_ALLOW_CONDITION_FILTER_KEY).is(filterName),
                        where(PLAN_RULE_DNY_CONDITION_FILTER_KEY).is(filterName)
                )
        );

        final Query query = query(criteria);
        final long count = operations.count(query, DeploymentPlan.class);
        final List<DeploymentPlan> plans = operations.find(query.with(pageable), DeploymentPlan.class);

        return new PageImpl<>(plans, pageable, count);
    }
}
