package com.glenneligio.service;

import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PropertiesServiceImpl implements PropertiesService{

    private static final Logger logger = LoggerFactory.getLogger(PropertiesServiceImpl.class);

    @Override
    public List<PropertiesFileEntry> getPropertiesFileEntriesFromPropertiesFile(String propertiesFileLocation) throws IOException {
        logger.info("Reading {}", propertiesFileLocation);
        List<PropertiesFileEntry> result = new ArrayList<>();

        String fileContent;
        try (InputStream inputStream = new FileInputStream(propertiesFileLocation)) {
            fileContent = new String(inputStream.readAllBytes());
            logger.debug("File content {}", fileContent);
            String[] entries = fileContent.split("\n");
            int lineNumberIndex = 1;

            for (String entry : entries) {
                if(entry.trim().isBlank()) {
                    logger.debug("Newline, skipped");
                    PropertiesFileEntry newLineEntry = new PropertiesFileEntry(null, null, null, false, false, lineNumberIndex,false);
                    result.add(newLineEntry);
                    lineNumberIndex++;
                    continue;
                }

                int equalsIndex = entry.indexOf("=");
                if (equalsIndex == -1) {
                    logger.debug("Invalid property entry: {}", entry);
                    PropertiesFileEntry invalidEntry = new PropertiesFileEntry(entry, null, null, false, false, lineNumberIndex, false);
                    result.add(invalidEntry);
                    lineNumberIndex++;
                    continue;
                }

                String propName = entry.substring(0, equalsIndex).trim();
                String propValue = entry.substring(equalsIndex + 1).trim();
                logger.debug("propName: {}", propName);
                logger.debug("propValue: {}", propValue);

                if (!propValue.startsWith("${")) {
                    logger.debug("Property entry is not being injected with environment variable, skipped");
                    PropertiesFileEntry skippedEntry = new PropertiesFileEntry(propName, null, propValue, true, false, lineNumberIndex, false);
                    result.add(skippedEntry);
                    lineNumberIndex++;
                    continue;
                }

                int colonIndex = propValue.indexOf(":");
                if (colonIndex == -1) {
                    logger.debug("Property entry does not have default value and is just injected, to be processed");
                    String injectedEnv = propValue.substring(2, propValue.length() - 1).trim();
                    PropertiesFileEntry injectedEntryWithNoDefaultValue = new PropertiesFileEntry(propName, injectedEnv, null, true, true, lineNumberIndex, false);
                    result.add(injectedEntryWithNoDefaultValue);
                    lineNumberIndex++;
                    continue;
                }

                String injectedEnv = propValue.substring(2, colonIndex).trim();
                String defaultValue = propValue.substring(colonIndex + 1, propValue.length() - 1);
                logger.debug("Property entry does have default value and is injected, to be processed");
                PropertiesFileEntry injectedEntryWithDefaultValue = new PropertiesFileEntry(propName, injectedEnv, defaultValue, true, true, lineNumberIndex, false);
                result.add(injectedEntryWithDefaultValue);
                lineNumberIndex++;
            }
            return result;
        }
    }



    @Override
    public void injectEnvFound(List<PropertiesFileEntry> propertiesFileEntries, String propertiesFileLocation) throws IOException {
        List<Integer> linesToBeInjected = propertiesFileEntries.stream()
                .filter(PropertiesFileEntry::isValid)
                .filter(PropertiesFileEntry::isValueInjected)
                .filter(entry -> !entry.isEnvValueSecret())
                .sorted(Comparator.comparingInt(PropertiesFileEntry::getLineNumber))
                .map(PropertiesFileEntry::getLineNumber)
                .toList();

        List<String> fileContents = Files.readAllLines(Path.of(propertiesFileLocation));
        for(int i = 1; i <= fileContents.size(); i++) {
            if(linesToBeInjected.contains(i)) {
                PropertiesFileEntry propEntry = propertiesFileEntries.get(i-1);
                String fileEntry = propEntry.getName() +
                        "=" +
                        "${" +
                        propEntry.getEnvUsed() +
                        ":" +
                        propEntry.getEnvValueToInject() +
                        "}";
                fileContents.set(i-1, fileEntry);
                propEntry.setInjected(true);
            }
        }

        Files.write(Path.of(propertiesFileLocation + "-injected"), fileContents, StandardCharsets.UTF_8);
    }

    @Override
    public void printReport(List<PropertiesFileEntry> propertiesFileEntries) {
        logger.info("Invalid entries or empty lines");
        propertiesFileEntries.stream()
                .filter(entry -> !entry.isValid())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries with no environment variable injected");
        propertiesFileEntries.stream()
                .filter(PropertiesFileEntry::isValid)
                .filter(entry -> !entry.isValueInjected())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries whose environment variable is not present in yaml");
        propertiesFileEntries.stream()
                .filter(PropertiesFileEntry::isValid)
                .filter(PropertiesFileEntry::isValueInjected)
                .filter(entry -> !entry.isPresentInYaml())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries that was injected with environment variables");
        propertiesFileEntries.stream()
                .filter(PropertiesFileEntry::isValid)
                .filter(PropertiesFileEntry::isInjected)
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries whose environment variable was a secret");
        propertiesFileEntries.stream()
                .filter(PropertiesFileEntry::isValid)
                .filter(PropertiesFileEntry::isEnvValueSecret)
                .filter(entry -> !entry.isInjected())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));
    }

    @Override
    public List<PropertiesFileEntry> addNewEnvFromYaml(List<PropertiesFileEntry> currentEnvFileEntries, List<YamlFileEnvEntry> yamlFileEnvEntries) {
        for(PropertiesFileEntry entry : currentEnvFileEntries) {
            for(YamlFileEnvEntry yamlEntry : yamlFileEnvEntries) {
                if(Objects.nonNull(entry.getEnvUsed()) && Objects.nonNull(yamlEntry.envName())) {
                    if(entry.getEnvUsed().equals(yamlEntry.envName()) && !yamlEntry.isSecret()) {
                        entry.setEnvValueToInject(yamlEntry.envValue());
                        entry.setEnvValueSecret(false);
                        entry.setPresentInYaml(true);
                    }
                    if(entry.getEnvUsed().equals(yamlEntry.envName()) && yamlEntry.isSecret()) {
                        entry.setEnvValueSecret(true);
                        entry.setPresentInYaml(true);
                    }
                }
            }
        }
        return currentEnvFileEntries;
    }
}
