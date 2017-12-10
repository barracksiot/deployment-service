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

package io.barracks.deploymentservice.rest;

import io.barracks.deploymentservice.manager.DeploymentPlanManager;
import io.barracks.deploymentservice.model.DeploymentPlan;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/owners/{userId}/plans")
public class DeploymentPlanResource {

    private final DeploymentPlanManager deploymentPlanManager;
    private final PagedResourcesAssembler<DeploymentPlan> assembler;


    public DeploymentPlanResource(DeploymentPlanManager deploymentPlanManager, PagedResourcesAssembler<DeploymentPlan> assembler) {
        this.deploymentPlanManager = deploymentPlanManager;
        this.assembler = assembler;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(method = RequestMethod.POST)
    public DeploymentPlan publishDeploymentPlan(
            @Valid @RequestBody DeploymentPlan deploymentPlan,
            @NotBlank @PathVariable("userId") String userId
    ) {
        return deploymentPlanManager.publishDeploymentPlan(
                deploymentPlan.toBuilder().userId(userId).build()
        );
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public PagedResources<Resource<DeploymentPlan>> getDeploymentPlansByFilterName(
            @RequestParam(value = "filter") String filterName,
            @NotBlank @PathVariable("userId") String userId,
            Pageable pageable
    ) {
        return assembler.toResource(
                deploymentPlanManager.getDeploymentPlansByFilterName(filterName, userId, pageable));
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{packageRef}/versions")
    public List<String> getDeployedVersions(
            @NotBlank @PathVariable("userId") String userId,
            @NotBlank @PathVariable("packageRef") String packageRef,
            @RequestParam(value = "onlyActive", required = false, defaultValue = "true") boolean onlyActive
    ) {
        return deploymentPlanManager.getDeployedVersions(userId, packageRef, onlyActive);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, path = "/{packageRef}")
    public DeploymentPlan getActiveDeploymentPlan(
            @NotBlank @PathVariable("userId") String userId,
            @NotBlank @PathVariable("packageRef") String packageRef
    ) {
        return deploymentPlanManager.getActiveDeploymentPlan(userId, packageRef);
    }

}
