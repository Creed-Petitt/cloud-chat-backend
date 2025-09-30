package com.creedpetitt.aiservicesbackend.aiservices;

import com.google.cloud.aiplatform.v1.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImagenService {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String storageBucket;

    public String generateImage(String prompt) throws IOException {
        String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .build();

        try (PredictionServiceClient client = PredictionServiceClient.create(settings)) {
            String modelName = String.format(
                    "projects/%s/locations/%s/publishers/google/models/imagen-3.0-generate-001",
                    projectId, location
            );

            // Parameters with Cloud Storage URI
            com.google.protobuf.Struct.Builder parametersBuilder = com.google.protobuf.Struct.newBuilder();
            parametersBuilder.putFields("sampleCount",
                com.google.protobuf.Value.newBuilder().setNumberValue(1).build());
            parametersBuilder.putFields("storageUri",
                com.google.protobuf.Value.newBuilder()
                    .setStringValue("gs://" + storageBucket + "/images/")
                    .build());

            // Instance with prompt
            com.google.protobuf.Struct.Builder instanceBuilder = com.google.protobuf.Struct.newBuilder();
            instanceBuilder.putFields("prompt",
                com.google.protobuf.Value.newBuilder().setStringValue(prompt).build());

            List<com.google.protobuf.Value> instances = new ArrayList<>();
            instances.add(com.google.protobuf.Value.newBuilder()
                .setStructValue(instanceBuilder.build())
                .build());

            PredictRequest request = PredictRequest.newBuilder()
                    .setEndpoint(modelName)
                    .addAllInstances(instances)
                    .setParameters(com.google.protobuf.Value.newBuilder()
                        .setStructValue(parametersBuilder.build())
                        .build())
                    .build();

            PredictResponse response = client.predict(request);

            if (response.getPredictionsCount() > 0) {
                com.google.protobuf.Struct predictionStruct = response.getPredictions(0).getStructValue();

                if (predictionStruct.containsFields("gcsUri")) {
                    String gcsUri = predictionStruct.getFieldsOrThrow("gcsUri").getStringValue();
                    return gcsUri.replace("gs://", "https://storage.googleapis.com/");
                }
            }

            throw new RuntimeException("No image URL in response");
        }
    }
}
