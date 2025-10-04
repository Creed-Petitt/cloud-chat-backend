package com.creedpetitt.aiservicesbackend.aiservices;

import java.io.IOException;

public interface ImageService {
    String generateImage(String prompt) throws IOException;
    String getImageModel();
}
