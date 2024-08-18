package com.glenneligio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.glenneligio.model.YamlFileEnvEntry;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.util.List;

public interface YamlService {
    List<YamlFileEnvEntry> getYamlFileEnvEntries(String yamlFileLocation, String containerImageName) throws FileNotFoundException, JsonProcessingException, AccessDeniedException;
}
