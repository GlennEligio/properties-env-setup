package com.glenneligio.service;

import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;

import java.io.IOException;
import java.util.List;

public interface PropertiesService {
    List<PropertiesFileEntry> getPropertiesFileEntriesFromPropertiesFile(String propertiesFileLocation) throws IOException;

    void injectEnvFound(List<PropertiesFileEntry> propertiesFileEntries, String propertiesFileLocation) throws IOException;
    void printReport(List<PropertiesFileEntry> propertiesFileEntries);
    List<PropertiesFileEntry> populateEnvFileEntriesWithValuesFromYaml(List<PropertiesFileEntry> currentEnvFileEntries, List<YamlFileEnvEntry> yamlFileEnvEntries);
}
