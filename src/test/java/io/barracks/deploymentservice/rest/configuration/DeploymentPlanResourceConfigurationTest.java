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

package io.barracks.deploymentservice.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.commons.util.Endpoint;
import io.barracks.deploymentservice.exception.UnknownDeploymentPlanException;
import io.barracks.deploymentservice.model.DeploymentPlan;
import io.barracks.deploymentservice.rest.BarracksResourceTest;
import io.barracks.deploymentservice.rest.DeploymentPlanResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.barracks.deploymentservice.utils.DeploymentPlanUtils.getDeploymentPlan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = DeploymentPlanResource.class, outputDir = "build/generated-snippets/deployment/plans")
public class DeploymentPlanResourceConfigurationTest {

    private static final Endpoint GET_DEPLOYMENT_PLANS_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans", "filter={filter}");
    private static final Endpoint GET_ACTIVE_DEPLOYMENT_PLAN_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans/{packageRef}");
    private static final Endpoint GET_DEPLOYED_VERSIONS_WITH_PARAMETER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans/{packageRef}/versions", "onlyActive={onlyActive}");
    private static final Endpoint GET_DEPLOYED_VERSIONS_DEFAULT_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/plans/{packageRef}/versions");

    private static final String baseUrl = "https://not.barracks.io";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeploymentPlanResource deploymentPlanResource;

    private static DeploymentPlan buildDeploymentPlanRequest() {
        return getDeploymentPlan().toBuilder()
                .id(null)
                .userId(null)
                .created(null)
                .build();
    }

    @Test
    public void documentCreateDeploymentPlan() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = "userId";
        final DeploymentPlan request = buildDeploymentPlanRequest();
        when(deploymentPlanResource.publishDeploymentPlan(request, userId)).thenReturn(request);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.post("/owners/{userId}/plans", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // Then
        result.andExpect(status().isCreated())
                .andDo(document(
                        "publish",
                        requestFields(
                                fieldWithPath("packageRef").description("The package's unique reference"),
                                fieldWithPath("allow.filters").description("The eligible filters for that package").optional(),
                                fieldWithPath("deny.filters").description("The filters rejected for that package").optional(),
                                fieldWithPath("rules").description("The rules to apply to the versions of the package").optional(),
                                fieldWithPath("rules[].version").description("The version for the rule"),
                                fieldWithPath("rules[].allow.filters").description("The eligible filters for that version").optional(),
                                fieldWithPath("rules[].deny.filters").description("The filters rejected for that version").optional()
                        ),
                        pathParameters(
                                parameterWithName("userId").description("The unique identifier of the owner")
                        )
                ));
        verify(deploymentPlanResource).publishDeploymentPlan(request, userId);
    }

    @Test
    public void publishDeploymentPlan_shouldValidateDeploymentPlan() throws Exception {
        // Given
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String userId = UUID.randomUUID().toString();
        final DeploymentPlan request = buildDeploymentPlanRequest().toBuilder().packageRef(null).build();

        // When
        final ResultActions result = mvc.perform(
                post("/owners/{userId}/plans", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // Then
        result.andExpect(status().isBadRequest());
        verifyZeroInteractions(deploymentPlanResource);
    }

    @Test
    public void publishDeploymentPlan_shouldRequireADeploymentPlanParam() throws Exception {
        // When
        final String userId = UUID.randomUUID().toString();
        final ResultActions result = mvc.perform(
                post("/owners/{userId}/plans", userId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isUnprocessableEntity());
        verifyZeroInteractions(deploymentPlanResource);
    }

    @Test
    public void documentGetDeploymentPlanByFilterName() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Endpoint endpoint = GET_DEPLOYMENT_PLANS_ENDPOINT;
        final String userId = "userId";
        final String filterName = "filterName";
        final DeploymentPlan deploymentPlan1 = getDeploymentPlan();
        final DeploymentPlan deploymentPlan2 = getDeploymentPlan();
        final Page<DeploymentPlan> page = new PageImpl<>(Lists.newArrayList(deploymentPlan1, deploymentPlan2));
        final PagedResources<Resource<DeploymentPlan>> expected = PagedResourcesUtils.<DeploymentPlan>getPagedResourcesAssembler().toResource(page);

        doReturn(expected).when(deploymentPlanResource).getDeploymentPlansByFilterName(eq(filterName), eq(userId), any(Pageable.class));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId)
                        .param("filter", filterName)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "get-by-filter-name",
                        pathParameters(
                                parameterWithName("userId").description("The unique identifier of the owner")
                        ),
                        requestParameters(
                                parameterWithName("filter").description("The unique identifier of the filter")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.deploymentPlans").description("The list of deployment Plans"),
                                fieldWithPath("_links").ignored(),
                                fieldWithPath("page").ignored()
                        )
                ));

        verify(deploymentPlanResource).getDeploymentPlansByFilterName(eq(filterName), eq(userId), any(Pageable.class));
    }

    @Test
    public void getDeploymentPlansByFilterName_whenAllIsFine_shouldCallResourceAndReturnPlansList() throws Exception {
        //Given
        final Endpoint endpoint = GET_DEPLOYMENT_PLANS_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filter = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final DeploymentPlan deploymentPlan1 = getDeploymentPlan();
        final DeploymentPlan deploymentPlan2 = getDeploymentPlan();
        final Page<DeploymentPlan> page = new PageImpl<>(Lists.newArrayList(deploymentPlan1, deploymentPlan2));
        final PagedResources<Resource<DeploymentPlan>> expected = PagedResourcesUtils.<DeploymentPlan>getPagedResourcesAssembler().toResource(page);

        when(deploymentPlanResource.getDeploymentPlansByFilterName(filter, userId, pageable)).thenReturn(expected);

        // When
        final ResultActions result = mvc.perform(
                request(
                        endpoint.getMethod(),
                        endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, filter)
                ).accept(MediaType.APPLICATION_JSON_UTF8));

        //Then
        verify(deploymentPlanResource).getDeploymentPlansByFilterName(filter, userId, pageable);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.deploymentPlans", hasSize(page.getNumberOfElements())))
                .andExpect(jsonPath("$._embedded.deploymentPlans[0].packageRef").value(deploymentPlan1.getPackageRef()))
                .andExpect(jsonPath("$._embedded.deploymentPlans[1].packageRef").value(deploymentPlan2.getPackageRef()));
    }

    @Test
    public void getActiveDeploymentPlan_whenAllIsFine_shouldCallResourceAndReturnPlan() throws Exception {
        //Given
        final Endpoint endpoint = GET_ACTIVE_DEPLOYMENT_PLAN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final DeploymentPlan deploymentPlan = getDeploymentPlan();
        when(deploymentPlanResource.getActiveDeploymentPlan(userId, packageRef)).thenReturn(deploymentPlan);

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8));

        //Then
        verify(deploymentPlanResource).getActiveDeploymentPlan(userId, packageRef);
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.packageRef").value(deploymentPlan.getPackageRef()))
                .andExpect(jsonPath("$.created").doesNotExist())
                .andExpect(jsonPath("$.rules").isArray())
                .andExpect(jsonPath("$.allow").exists())
                .andExpect(jsonPath("$.allow.filters").isArray());
    }

    @Test
    public void getActiveDeploymentPlan_whenUnknownDeploymentPlanException_shouldReturn404() throws Exception {
        //Given
        final Endpoint endpoint = GET_ACTIVE_DEPLOYMENT_PLAN_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        when(deploymentPlanResource.getActiveDeploymentPlan(userId, packageRef))
                .thenThrow(new UnknownDeploymentPlanException(userId, packageRef));

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8));

        //Then
        verify(deploymentPlanResource).getActiveDeploymentPlan(userId, packageRef);
        result.andExpect(status().isNotFound());
    }

    @Test
    public void documentGetActiveDeploymentPlan() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Endpoint endpoint = GET_ACTIVE_DEPLOYMENT_PLAN_ENDPOINT;
        final String userId = "userId";
        final String packageRef = "packageRef";
        final DeploymentPlan deploymentPlan = getDeploymentPlan();
        when(deploymentPlanResource.getActiveDeploymentPlan(userId, packageRef))
                .thenReturn(deploymentPlan);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId, packageRef)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "get-active-deployment-plan",
                        pathParameters(
                                parameterWithName("userId").description("The unique identifier of the owner"),
                                parameterWithName("packageRef").description("The reference of the package")
                        ),
                        responseFields(
                                fieldWithPath("packageRef").description("The packages's unique reference"),
                                fieldWithPath("allow.filters").description("The eligible filters for that package").optional(),
                                fieldWithPath("deny.filters").description("The filters rejected for that package").optional(),
                                fieldWithPath("rules").description("The rules to apply to the versions of the package").optional(),
                                fieldWithPath("rules[].version").description("The version for the rule"),
                                fieldWithPath("rules[].allow.filters").description("The eligible filters for that version").optional(),
                                fieldWithPath("rules[].deny.filters").description("The filters rejected for that version").optional()
                        )
                ));
        verify(deploymentPlanResource).getActiveDeploymentPlan(userId, packageRef);
    }

    @Test
    public void documentGetDeployedVersions() throws Exception {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Endpoint endpoint = GET_DEPLOYED_VERSIONS_WITH_PARAMETER_ENDPOINT;
        final String userId = "userId";
        final String packageRef = "packageRef";
        final List<String> deployedVersions = Arrays.asList("version1", "version2", "version3");

        when(deploymentPlanResource.getDeployedVersions(userId, packageRef, true))
                .thenReturn(deployedVersions);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), userId, packageRef)
                        .param("onlyActive", "true")
                        .accept(MediaType.ALL)
        );

        // Then
        result.andExpect(status().isOk())
                .andDo(document(
                        "get-deployed-versions",
                        pathParameters(
                                parameterWithName("userId").description("The unique identifier of the owner"),
                                parameterWithName("packageRef").description("The reference of the package")
                        )
                        , requestParameters(
                                parameterWithName("onlyActive").description("Boolean to set whether only active plan should be explored")
                        )
                ));
        verify(deploymentPlanResource).getDeployedVersions(userId, packageRef, true);
    }

    @Test
    public void getDeployedVersions_whenOnlyActiveParameterIsFalse_shouldUseParameterByDefault() throws Exception {
        //Given
        final Endpoint endpoint = GET_DEPLOYED_VERSIONS_WITH_PARAMETER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        when(deploymentPlanResource.getDeployedVersions(userId, packageRef, false))
                .thenReturn(Collections.emptyList());

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, packageRef, false))
                        .accept(MediaType.APPLICATION_JSON_UTF8));

        //Then
        verify(deploymentPlanResource).getDeployedVersions(userId, packageRef, false);
        result.andExpect(status().isOk());
    }

    @Test
    public void getDeployedVersions_whenOnlyActiveNotSet_shouldUseTrueByDefault() throws Exception {
        //Given
        final Endpoint endpoint = GET_DEPLOYED_VERSIONS_DEFAULT_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        when(deploymentPlanResource.getDeployedVersions(userId, packageRef, true))
                .thenReturn(Collections.emptyList());

        // When
        final ResultActions result = mvc.perform(
                request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI(userId, packageRef))
                        .accept(MediaType.APPLICATION_JSON_UTF8));

        //Then
        verify(deploymentPlanResource).getDeployedVersions(userId, packageRef, true);
        result.andExpect(status().isOk());
    }
}
