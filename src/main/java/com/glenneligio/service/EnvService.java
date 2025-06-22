package com.glenneligio.service;

import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface EnvService {

    List<EnvFileEntry> readOrCreateEnvFile(String envFileLocation) throws IOException;
    void injectEnvFound(List<EnvFileEntry> envFileEntries) throws IOException;
    void printReport(List<EnvFileEntry> envFileEntries);
    List<EnvFileEntry> addNewEnvFromYaml(List<EnvFileEntry> currentEnvFileEntries, List<YamlFileEnvEntry> yamlFileEnvEntries);
    List<EnvFileEntry> populateEnvFileEntriesWithValuesFromYaml(List<EnvFileEntry> currentEnvFileEnties, List<YamlFileEnvEntry> yamlFileEnvEntries);
    void setEnvFile(File file);
}
