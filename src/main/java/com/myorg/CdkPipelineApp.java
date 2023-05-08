package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class CdkPipelineApp {
    public static void main(final String[] args) {
        App app = new App();

        new CdkPipelineStack(app, "CdkPipelineStack", StackProps.builder()
                .env(Environment.builder()
                        .account("078889211630")
                        .region("us-west-1")
                        .build())
                .build());

        app.synth();
    }
}

