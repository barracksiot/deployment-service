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
import io.barracks.deploymentservice.utils.DeploymentPlanUtils;
import io.barracks.deploymentservice.utils.PackageUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static io.barracks.deploymentservice.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentPlanManagerTest {

    @Mock
    private DeploymentPlanRepository deploymentPlanRepository;

    @Mock
    private FilterServiceClient filterServiceClient;

    @Mock
    private ComponentServiceClient componentServiceClient;

    @Spy
    @InjectMocks
    private DeploymentPlanManager deploymentPlanManager;

    @Test
    public void publishDeploymentPlan_shouldValidateAndSavePlan_andReturnResult() {
        // Given
        final DeploymentPlan deploymentPlan = DeploymentPlanUtils.getDeploymentPlan();
        final DeploymentPlan expected = DeploymentPlanUtils.getDeploymentPlan();
        doNothing().when(deploymentPlanManager).validateDeploymentPlanFilters(deploymentPlan);
        doNothing().when(deploymentPlanManager).validateDeploymentPlanVersions(deploymentPlan);
        doNothing().when(deploymentPlanManager).validateDeploymentPlanPackage(deploymentPlan);
        when(deploymentPlanRepository.insert(deploymentPlan)).thenReturn(expected);

        // When
        final DeploymentPlan result = deploymentPlanManager.publishDeploymentPlan(deploymentPlan);

        // Then
        verify(deploymentPlanManager).validateDeploymentPlanFilters(deploymentPlan);
        verify(deploymentPlanManager).validateDeploymentPlanVersions(deploymentPlan);
        verify(deploymentPlanManager).validateDeploymentPlanPackage(deploymentPlan);
        verify(deploymentPlanRepository).insert(deploymentPlan);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void validateDeploymentPlanFilters_shouldThrowAnException_whenOneFilterDoesNotExist() {
        // Given
        final String userId = "User ID";
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        final String existingFilter = "filter1";
        final String unknownFilter = "filter2";
        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.extractFilters()).thenReturn(Arrays.asList(existingFilter, unknownFilter));
        when(filterServiceClient.filterExists(userId, existingFilter)).thenReturn(true);
        when(filterServiceClient.filterExists(userId, unknownFilter)).thenReturn(false);

        // When / Then
        assertThatExceptionOfType(InvalidFiltersException.class)
                .isThrownBy(() -> deploymentPlanManager.validateDeploymentPlanFilters(deploymentPlan))
                .withMessageContaining(unknownFilter)
                .withMessageContaining(userId);
        verify(filterServiceClient).filterExists(userId, existingFilter);
        verify(filterServiceClient).filterExists(userId, unknownFilter);
    }

    @Test
    public void validateDeploymentPlanFilters_shouldNotThrowAnException_whenAllFiltersExist() {
        // Given
        final String userId = "User ID";
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        final List<String> existingFilters = Arrays.asList("filter1", "filter2");
        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.extractFilters()).thenReturn(existingFilters);
        existingFilters.forEach(filter -> {
            when(filterServiceClient.filterExists(userId, filter)).thenReturn(true);
        });

        // When
        deploymentPlanManager.validateDeploymentPlanFilters(deploymentPlan);

        // Then
        verify(filterServiceClient).filterExists(userId, existingFilters.get(0));
        verify(filterServiceClient).filterExists(userId, existingFilters.get(1));
    }

    @Test
    public void validateDeploymentPlanVersions_shouldThrowAnException_whenOneVersionDoesNotExist() {
        // Given
        final String userId = "User ID";
        final String packageRef = "io.barracks.app1";
        final String existingVersion = "v0.0.1";
        final String unknownVersion = "v0.0.2";
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.getPackageRef()).thenReturn(packageRef);
        when(deploymentPlan.extractVersions()).thenReturn(Arrays.asList(existingVersion, unknownVersion));
        when(componentServiceClient.versionExists(userId, packageRef, existingVersion)).thenReturn(true);
        when(componentServiceClient.versionExists(userId, packageRef, unknownVersion)).thenReturn(false);

        // When / Then
        assertThatExceptionOfType(InvalidVersionsException.class)
                .isThrownBy(() -> deploymentPlanManager.validateDeploymentPlanVersions(deploymentPlan))
                .withMessageContaining(unknownVersion)
                .withMessageContaining(packageRef)
                .withMessageContaining(userId);
        verify(componentServiceClient).versionExists(userId, packageRef, existingVersion);
        verify(componentServiceClient).versionExists(userId, packageRef, unknownVersion);
    }

    @Test
    public void validateDeploymentPlanFilters_shouldNotThrowAnException_whenAllVersionsExist() {
        // Given
        final String userId = "User ID";
        final String packageRef = "io.barracks.app1";
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        final List<String> existingVersions = Arrays.asList("v0.0.1", "v0.0.2");
        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.getPackageRef()).thenReturn(packageRef);
        when(deploymentPlan.extractVersions()).thenReturn(existingVersions);
        existingVersions.forEach(version -> {
            when(componentServiceClient.versionExists(userId, packageRef, version)).thenReturn(true);
        });

        // When
        deploymentPlanManager.validateDeploymentPlanVersions(deploymentPlan);

        // Then
        verify(componentServiceClient).versionExists(userId, packageRef, existingVersions.get(0));
        verify(componentServiceClient).versionExists(userId, packageRef, existingVersions.get(1));
    }


    @Test
    public void validateDeploymentPlanPackage_shouldThrowAnException_whenPackageDoesNotExist() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);

        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.getPackageRef()).thenReturn(packageRef);
        doReturn(false).when(componentServiceClient).packageExists(userId, packageRef);

        // When / Then
        assertThatExceptionOfType(InvalidPackageException.class)
                .isThrownBy(() -> deploymentPlanManager.validateDeploymentPlanPackage(deploymentPlan))
                .withMessageContaining(userId)
                .withMessageContaining(packageRef);

        verify(componentServiceClient).packageExists(userId, packageRef);
    }

    @Test
    public void validateDeploymentPlanPackage_shouldNotThrowAnException_whenPackageExists() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);

        when(deploymentPlan.getUserId()).thenReturn(userId);
        when(deploymentPlan.getPackageRef()).thenReturn(packageRef);
        doReturn(true).when(componentServiceClient).packageExists(userId, packageRef);

        // When
        deploymentPlanManager.validateDeploymentPlanPackage(deploymentPlan);

        // Then
        verify(componentServiceClient).packageExists(userId, packageRef);
    }

    @Test
    public void isVersionAvailable_shouldReturnTrue_whenNoDenyOrAllow() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        doReturn(Optional.empty()).when(rule).getAllow();
        doReturn(Optional.empty()).when(rule).getDeny();

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verifyZeroInteractions(componentServiceClient);
        assertThat(result).isTrue();
    }

    @Test
    public void isVersionAvailable_shouldReturnTrue_whenAllowAndNoDeny() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.empty()).when(rule).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        assertThat(result).isTrue();
    }

    @Test
    public void isVersionAvailable_shouldReturnTrue_whenAllowedAndNotDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.of(deny)).when(rule).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isTrue();
    }

    @Test
    public void isVersionAvailable_shouldReturnFalse_whenAllowedAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.of(deny)).when(rule).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isVersionAvailable_shouldReturnFalse_whenNoAllowAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.empty()).when(rule).getAllow();
        doReturn(Optional.of(deny)).when(rule).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isVersionAvailable_shouldReturnFalse_whenNotAllowedAndNoDeny() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.empty()).when(rule).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isVersionAvailable_shouldReturnFalse_whenNotAllowedAndNotDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.of(deny)).when(rule).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient, atLeast(0)).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isVersionAvailable_shouldReturnFalse_whenNotAllowedAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentRule rule = mock(DeploymentRule.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(rule).getAllow();
        doReturn(Optional.of(deny)).when(rule).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isVersionAvailable(request, rule);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient, atLeast(0)).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isPackageAvailable_shouldReturnTrue_whenNoDenyOrAllow() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        doReturn(Optional.empty()).when(plan).getAllow();
        doReturn(Optional.empty()).when(plan).getDeny();

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verifyZeroInteractions(componentServiceClient);
        assertThat(result).isTrue();
    }

    @Test
    public void isPackageAvailable_shouldReturnTrue_whenAllowAndNoDeny() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.empty()).when(plan).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        assertThat(result).isTrue();
    }

    @Test
    public void isPackageAvailable_shouldReturnTrue_whenAllowedAndNotDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.of(deny)).when(plan).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isTrue();
    }

    @Test
    public void isPackageAvailable_shouldReturnFalse_whenAllowedAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.of(deny)).when(plan).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isPackageAvailable_shouldReturnFalse_whenNoAllowAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.empty()).when(plan).getAllow();
        doReturn(Optional.of(deny)).when(plan).getDeny();
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isPackageAvailable_shouldReturnFalse_whenNotAllowedAndNoDeny() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.empty()).when(plan).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isPackageAvailable_shouldReturnFalse_whenNotAllowedAndNotDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.of(deny)).when(plan).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient, atLeast(0)).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void isPackageAvailable_shouldReturnFalse_whenNotAllowedAndDenied() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final DeploymentCondition allow = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        final DeploymentCondition deny = DeploymentCondition.builder().filters(Collections.singletonList(UUID.randomUUID().toString())).build();
        doReturn(Optional.of(allow)).when(plan).getAllow();
        doReturn(Optional.of(deny)).when(plan).getDeny();
        doReturn(false).when(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        doReturn(true).when(filterServiceClient).isRequestMatchingFilters(request, deny.getFilters());

        // When
        final boolean result = deploymentPlanManager.isPackageAvailable(request, plan);

        // Then
        verify(filterServiceClient).isRequestMatchingFilters(request, allow.getFilters());
        verify(filterServiceClient, atLeast(0)).isRequestMatchingFilters(request, deny.getFilters());
        assertThat(result).isFalse();
    }

    @Test
    public void getPackageVersion_whenNoRules_shouldReturnEmpty() {
        // Given
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        doReturn(Collections.emptyList()).when(plan).getDeploymentRules();
        final DeviceRequest request = getDeviceRequest();

        // When
        final Optional<String> result = deploymentPlanManager.getPackageVersion(request, plan);

        // Then
        verify(plan).getDeploymentRules();
        verify(deploymentPlanManager).getPackageVersion(request, plan);
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).isEmpty();
    }

    @Test
    public void getPackageVersion_whenNoVersionAllowed_shouldReturnEmpty() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final List<DeploymentRule> rules = Arrays.asList(
                mock(DeploymentRule.class),
                mock(DeploymentRule.class)
        );
        rules.forEach(rule -> {
            doReturn(false).when(deploymentPlanManager).isVersionAvailable(request, rule);
            when(rule.getVersionId()).thenReturn(UUID.randomUUID().toString());
        });
        doReturn(rules).when(plan).getDeploymentRules();

        // When
        final Optional<String> result = deploymentPlanManager.getPackageVersion(request, plan);

        // Then
        verify(plan).getDeploymentRules();
        verify(deploymentPlanManager).getPackageVersion(request, plan);
        verify(deploymentPlanManager, times(rules.size())).isVersionAvailable(eq(request), any());
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).isEmpty();
    }

    @Test
    public void getPackageVersion_whenVersionsAllowed_shouldReturnFirstMatching() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        final List<DeploymentRule> rules = Arrays.asList(
                mock(DeploymentRule.class),
                mock(DeploymentRule.class)
        );
        rules.forEach(rule -> {
            doReturn(true).when(deploymentPlanManager).isVersionAvailable(request, rule);
            when(rule.getVersionId()).thenReturn(UUID.randomUUID().toString());
        });
        doReturn(rules).when(plan).getDeploymentRules();

        // When
        final Optional<String> result = deploymentPlanManager.getPackageVersion(request, plan);

        // Then
        verify(plan).getDeploymentRules();
        verify(deploymentPlanManager).getPackageVersion(request, plan);
        verify(deploymentPlanManager).isVersionAvailable(request, rules.get(0));
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).isPresent().contains(rules.get(0).getVersionId());
    }

    @Test
    public void getPackageForPlan_whenPlanNotAllowed_shouldReturnEmpty() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final String reference = UUID.randomUUID().toString();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        when(plan.getPackageRef()).thenReturn(reference);
        doReturn(false).when(deploymentPlanManager).isPackageAvailable(request, plan);

        // When
        final Optional<Package> result = deploymentPlanManager.getPackageForPlan(request, plan);

        // Then
        verify(deploymentPlanManager).getPackageForPlan(request, plan);
        verify(deploymentPlanManager).isPackageAvailable(request, plan);
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).isEmpty();
    }

    @Test
    public void getPackageForPlan_whenNoVersion_shouldReturnPackageWithNoVersion() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final String reference = UUID.randomUUID().toString();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        when(plan.getPackageRef()).thenReturn(reference);
        doReturn(true).when(deploymentPlanManager).isPackageAvailable(request, plan);
        doReturn(Optional.empty()).when(deploymentPlanManager).getPackageVersion(request, plan);
        final Package expected = Package.builder().reference(reference).build();

        // When
        final Optional<Package> result = deploymentPlanManager.getPackageForPlan(request, plan);

        // Then
        verify(deploymentPlanManager).getPackageForPlan(request, plan);
        verify(deploymentPlanManager).isPackageAvailable(request, plan);
        verify(deploymentPlanManager).getPackageVersion(request, plan);
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).contains(expected);
    }

    @Test
    public void getPackageForPlan_whenVersion_shouldReturnPackageWithVersion() {
        // Given
        final DeviceRequest request = getDeviceRequest();
        final String reference = UUID.randomUUID().toString();
        final String version = UUID.randomUUID().toString();
        final DeploymentPlan plan = mock(DeploymentPlan.class);
        when(plan.getPackageRef()).thenReturn(reference);
        doReturn(true).when(deploymentPlanManager).isPackageAvailable(request, plan);
        doReturn(Optional.of(version)).when(deploymentPlanManager).getPackageVersion(request, plan);
        final Package expected = Package.builder().reference(reference).version(version).build();

        // When
        final Optional<Package> result = deploymentPlanManager.getPackageForPlan(request, plan);

        // Then
        verify(deploymentPlanManager).getPackageForPlan(request, plan);
        verify(deploymentPlanManager).isPackageAvailable(request, plan);
        verify(deploymentPlanManager).getPackageVersion(request, plan);
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).contains(expected);
    }

    @Test
    public void getPackagesForDeviceRequest_shouldCheckRequestForEachPlan_andReturnResult() {
        // Given
        final List<DeploymentPlan> plans = Arrays.asList(
                DeploymentPlanUtils.getDeploymentPlan(),
                DeploymentPlanUtils.getDeploymentPlan()
        );
        final DeviceRequest request = getDeviceRequest();
        final String userId = request.getUserId();
        final Package available = PackageUtils.getPackage();
        doReturn(plans).when(deploymentPlanRepository).findByUserId(userId);
        doReturn(Optional.empty()).when(deploymentPlanManager).getPackageForPlan(request, plans.get(0));
        doReturn(Optional.of(available)).when(deploymentPlanManager).getPackageForPlan(request, plans.get(1));

        // When
        final ResolvedPackages result = deploymentPlanManager.resolvePackagesForDeviceRequest(request);

        // Then
        verify(deploymentPlanRepository).findByUserId(userId);
        verify(deploymentPlanManager).resolvePackagesForDeviceRequest(request);
        verify(deploymentPlanManager).getPackageForPlan(request, plans.get(0));
        verify(deploymentPlanManager).getPackageForPlan(request, plans.get(1));
        verifyNoMoreInteractions(deploymentPlanManager);
        assertThat(result).isNotNull();
        assertThat(result.getAbsents()).containsOnly(Package.builder().reference(plans.get(0).getPackageRef()).build());
        assertThat(result.getPresents()).containsOnly(available);
    }

    @Test
    public void getDeploymentPlansByFilterName_shouldCallRepository_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final List<DeploymentPlan> plans = Arrays.asList(
                DeploymentPlanUtils.getDeploymentPlan().toBuilder().userId(userId).build(),
                DeploymentPlanUtils.getDeploymentPlan().toBuilder().userId(userId).build()
        );

        final Page<DeploymentPlan> expected = new PageImpl<DeploymentPlan>(plans, pageable, plans.size());

        doReturn(expected).when(deploymentPlanRepository).findByFilterNameAndUserId(userId, filterName, pageable);

        // When
        final Page<DeploymentPlan> result = deploymentPlanManager.getDeploymentPlansByFilterName(filterName, userId, pageable);

        // Then
        verify(deploymentPlanRepository).findByFilterNameAndUserId(userId, filterName, pageable);

        assertThat(result).containsOnlyElementsOf(plans);
    }

    @Test
    public void getActiveDeploymentPlan_whenOptionalIsEmpty_shouldThrowAnException() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        doReturn(Optional.empty()).when(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);

        // When / Then
        assertThatExceptionOfType(UnknownDeploymentPlanException.class)
                .isThrownBy(() -> deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef));
        verify(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);
    }

    @Test
    public void getActiveDeploymentPlan_whenOptionalIsPresent_shouldReturnTheContent() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = mock(DeploymentPlan.class);
        doReturn(Optional.of(deploymentPlan)).when(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);

        // When
        final DeploymentPlan result = deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef);

        // Then
        verify(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);
        assertThat(result).isNotNull().isEqualTo(deploymentPlan);
    }

    @Test
    public void getDeployedVersions_whenOnlyActiveRequired_shouldReturnVersionsFromActivePlan() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan activePlan = DeploymentPlanUtils.getDeploymentPlan();
        doReturn(Optional.of(activePlan)).when(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);

        // When
        final List<String> result = deploymentPlanManager.getDeployedVersions(userId, packageRef, true);

        // Then
        verify(deploymentPlanRepository).getActiveDeploymentPlan(userId, packageRef);
        assertThat(result).isNotNull().isEqualTo(activePlan.extractVersions());
    }

    @Test
    public void getDeployedVersions_whenNotOnlyActiveRequired_shouldReturnAllDeployedVersions() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final List<String> deployedVersions = new ArrayList<>();
        deployedVersions.add("version1");
        deployedVersions.add("version2");

        doReturn(deployedVersions).when(deploymentPlanRepository).getDeployedVersions(userId, packageRef);

        // When
        final List<String> result = deploymentPlanManager.getDeployedVersions(userId, packageRef, false);

        // Then
        verify(deploymentPlanRepository).getDeployedVersions(userId, packageRef);
        assertThat(result).isNotNull().isEqualTo(deployedVersions);
    }
}