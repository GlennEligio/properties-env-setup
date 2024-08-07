package com.glenneligio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glenneligio.model.YamlFileEnvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YamlServiceImpl implements YamlService {

    private static final Logger logger = LoggerFactory.getLogger(YamlServiceImpl.class);

    @Override
    public List<YamlFileEnvEntry> getYamlFileEnvEntries(String yamlFileLocation, String containerImageName) throws FileNotFoundException, JsonProcessingException {
        logger.info("Reading yaml file {}", yamlFileLocation);
        List<YamlFileEnvEntry> result = new ArrayList<>();
        String containersPath = "spec.template.spec.containers";
        Yaml yaml = new Yaml();
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = new FileInputStream(yamlFileLocation);
        Map<String, Object> deploymentYaml = yaml.load(inputStream);
        String deploymentJson = objectMapper.writeValueAsString(deploymentYaml);
        JsonNode objectNode = objectMapper.readTree(deploymentJson);
        JsonNode containerArrayNode = getNestedField(containersPath, objectNode);
        if(Objects.nonNull(containerArrayNode) && containerArrayNode.isArray()) {
            for(JsonNode elementNode : containerArrayNode) {
                String imageName = elementNode.get("image").asText();
                if(imageName.equals(containerImageName)) {
                    JsonNode envArrayNode = elementNode.get("env");
                    if(Objects.nonNull(envArrayNode) && envArrayNode.isArray()) {
                        for(JsonNode envNode : envArrayNode) {
                            String envName = envNode.get("name").asText();
                            JsonNode value = envNode.get("value");
                            JsonNode valueFrom = envNode.get("valueFrom");
                            if(Objects.nonNull(value)) {
                                YamlFileEnvEntry envEntry = new YamlFileEnvEntry(envName, value.asText(), false);
                                result.add(envEntry);
                            }
                            if(Objects.nonNull(valueFrom)) {
                                YamlFileEnvEntry envEntry = new YamlFileEnvEntry(envName, null, true);
                                result.add(envEntry);
                            }
                        }
                    }
                }
            }
        }
        logger.debug("Map object representation of yaml file {}", deploymentYaml);
        return result;
    }

    private JsonNode getNestedField(String nestedFieldKey, JsonNode jsonNode) {
        String[] fieldTree = nestedFieldKey.trim().split("\\.");
        JsonNode currentNode = jsonNode;

        for(String field : fieldTree) {
            if(Objects.isNull(currentNode)) {
                break;
            }
            currentNode = currentNode.get(field);
        }

        return currentNode;
    }
}
