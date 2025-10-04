package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImageServiceFactory {
    private final Map<String, ImageService> imageServiceMap;

    public ImageServiceFactory(List<ImageService> imageServices) {
        this.imageServiceMap = imageServices.stream()
                .collect(Collectors.toMap(ImageService::getImageModel, Function.identity()));
    }

    public ImageService getImageService(String model) {
        return imageServiceMap.get(model);
    }
}
