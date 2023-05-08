package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.pipelines.*;
import software.amazon.awscdk.services.codebuild.IProject;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codepipeline.Action;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubTrigger;
import software.constructs.Construct;

import java.util.Arrays;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class CdkPipelineStack extends Stack {

    public CdkPipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Create a new CodePipeline
        Pipeline pipeline = Pipeline.Builder.create(this, "CDK-CodePipeline")
                .pipelineName("CDK-CodePipeline")
                .build();

        // Add source stage
        StageOptions sourceStageOptions = StageOptions.builder()
                .stageName("Source")
                .actions(Arrays.asList(createSourceAction()))
                .build();

        pipeline.addStage(sourceStageOptions);

        // Add build stage
        StageOptions buildStageOptions = StageOptions.builder()
                .stageName("Build")
                .actions(Arrays.asList(createBuildAction()))
                .build();

        pipeline.addStage(buildStageOptions);

        // Output the CodePipeline ARN
        CfnOutput.Builder.create(this, "CodePipelineArnOutput")
                .value(pipeline.getPipelineArn())
                .description("CodePipeline ARN")
                .build();
    }

    private Action createSourceAction() {
        // Create a new GitHub source action
        return GitHubSourceAction.Builder.create()
                .actionName("GitHub_Source")
                .output(Artifact.artifact("SourceArtifact"))
                .oauthToken(SecretValue.secretsManager("github-token")) // Replace with your GitHub token secret ID
                .owner("rpham-aptive")
                .repo("IP-Address-Validation")
                .branch("main")
                .trigger(GitHubTrigger.WEBHOOK) // Poll for changes in the repository
                .build();
    }

    private Action createBuildAction() {
        // Reference your pre-existing CodeBuild project
        IProject existingProject = Project.fromProjectName(this, "ExistingCodeBuildProject", "CDK-Codebuild-Project");

        // Create a new CodeBuild action
        return CodeBuildAction.Builder.create()
                .actionName("CodeBuild_Build")
                .input(Artifact.artifact("SourceArtifact"))
                .project(existingProject)
                .build();
    }
}