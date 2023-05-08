package com.myorg;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.List;

public class ApiGatewayStack extends Stack {

    public ApiGatewayStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Reference the pre-existing VPC
        IVpc existingVpc = Vpc.fromLookup(this, "ExistingVpc", VpcLookupOptions.builder()
                .vpcId("vpc-0c69559c73a89b107") // Replace with your VPC ID
                .build());

        // Create a new CodeBuild project
        IBucket existingBucket = Bucket.fromBucketName(this, "ExistingBucket", "ip-address-service-deployment");
        Project codeBuildProject = Project.Builder.create(this, "CDK-Codebuild-Project")
                .projectName("CDK-Codebuild-Project")
                .source(Source.gitHub(GitHubSourceProps.builder()
                        .owner("rpham-aptive")
                        .repo("IP-Address-Validation")
                        .branchOrRef("main")
                        .cloneDepth(1)
                        .fetchSubmodules(false)
                        .webhook(true)
                        .build()))
                .artifacts(Artifacts.s3(S3ArtifactsProps.builder()
                        .bucket(existingBucket)
                        .path("Build")
                        .packageZip(false)
                        .includeBuildId(false)
                        .build()))
                .cache(Cache.local(LocalCacheMode.SOURCE))
                .build();

        ISecurityGroup existingSecurityGroup = SecurityGroup.fromSecurityGroupId(this, "ExistingSecurityGroup", "sg-0021385f69c5ef261");
        List<ISecurityGroup> securityGroups = new ArrayList<>();
        securityGroups.add(existingSecurityGroup);
        // Create a new Lambda function
        Function lambdaFunction = Function.Builder.create(this, "MyLambdaFunction")
                .runtime(Runtime.JAVA_11)
                .handler("org.example.lambda.LambdaHandler")
                .functionName("CDK-IPAddressService")
                .timeout(Duration.minutes(5))
                .vpc(existingVpc)
                .securityGroups(securityGroups) // Assign the existing security group
                .allowPublicSubnet(true)
                .code(Code.fromAsset("build/distributions/")) // Specify the directory where your Lambda function code is located
                .build();

        List<EndpointType> endpointType = new ArrayList<>();
        endpointType.add(EndpointType.REGIONAL);
        // Create a new REST API
        RestApi restApi = RestApi.Builder.create(this, "MyRestApi")
                .restApiName("CDK-API-Gateway")
                .description("API Gateway generated via CDK.")
                .endpointTypes(endpointType)
                .build();

        // Create a new resource under the API root
        Resource apiResource = restApi.getRoot().addResource("blacklist");

        // Create a new GET method for the resource
        apiResource.addMethod("GET", new LambdaIntegration(lambdaFunction));
        apiResource.addMethod("POST", new LambdaIntegration(lambdaFunction));

        // Output the API endpoint URL
        CfnOutput.Builder.create(this, "ApiEndpointOutput")
                .value(restApi.getUrl())
                .description("API Gateway endpoint URL")
                .build();
    }
}