package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.Parameter
import com.fieldju.gradle.plugins.lambdasam.LambdaSamExtension
import com.fieldju.gradle.plugins.lambdasam.LambdaSamPlugin
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import org.gradle.api.tasks.TaskAction

class DeploySamTask extends SamTask {
    static final String TASK_GROUP = 'LambdaSam'

    DeploySamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @TaskAction
    void taskAction() {
        def config = project.extensions.getByName(LambdaSamPlugin.EXTENSION_NAME) as LambdaSamExtension

        CloudFormationDeployer deployer = new CloudFormationDeployer(
                AmazonCloudFormationClient.builder()
                        .standard()
                        .withRegion(Regions.fromName(config.getRegion()))
                        .build() as AmazonCloudFormationClient
        )

        String stackName = config.getStackName()
        String samTemplate = new File("${project.buildDir.absolutePath}${File.separator}sam${File.separator}sam-deploy.yaml").text
        Set<Parameter> parameterOverrides = config.getParameterOverrides()
        Set<String> capabilities = config.getCapabilities()

        String changeSetName = deployer.createAndWaitForChangeSet(stackName, samTemplate, parameterOverrides, capabilities)

        def executeChangeset = true
        if (executeChangeset) {
            deployer.executeChangeSet(changeSetName, stackName)
            deployer.waitForExecute(changeSetName, stackName)

            logger.lifecycle("Successfully executed change set ${changeSetName} for stack name: ${stackName}")
        } else {
            deployer.logChangeSetDescription(changeSetName, stackName)
        }
    }
}
