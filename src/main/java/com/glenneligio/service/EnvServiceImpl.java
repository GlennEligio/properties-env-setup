package com.glenneligio.service;

import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EnvServiceImpl implements EnvService {

    private static final Logger logger = LoggerFactory.getLogger(EnvServiceImpl.class);

    @Override
    public List<EnvFileEntry> readOrCreateEnvFile(String envFileLocation) throws IOException {
        List<EnvFileEntry> envFileEntries = new ArrayList<>();
        File envFile = new File(envFileLocation);
        if(!envFile.exists() && envFile.createNewFile()) {
           logger.info("Env file does not exist, created a new one");
           return envFileEntries;
        }

        List<String> envFileLines = Files.readAllLines(Path.of(envFileLocation));
        int lineNumber = 1;

        for(String envFileEntry : envFileLines) {
            if(envFileEntry.trim().isBlank()) {
                logger.debug("Empty line, skipped");
                EnvFileEntry entry = new EnvFileEntry(envFileEntry, null,null, false, false, lineNumber, false, false, false);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            int equalsIndex = envFileEntry.indexOf("=");
            if(equalsIndex == -1) {
                logger.debug("Invalid entry, skipped: {}", envFileEntry);
                EnvFileEntry entry = new EnvFileEntry(envFileEntry, null, null, false, false, lineNumber, false, false, false);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            String envName = envFileEntry.substring(0, equalsIndex).trim();
            String envValue = envFileEntry.substring(equalsIndex + 1).trim();
            logger.debug("envName: {}", envName);
            logger.debug("envValue: {}", envValue);

            if(envValue.trim().isBlank()) {
                logger.debug("Entry have no default value: {}", envName);
                EnvFileEntry entry = new EnvFileEntry(envName, "", null, true, false, lineNumber, false, false, false);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            logger.debug("Entry have default value: {}", envName);
            EnvFileEntry entry = new EnvFileEntry(envName, envValue, null, true, false, lineNumber, false, false, false);
            envFileEntries.add(entry);
            lineNumber++;
        }

        return envFileEntries;
    }

    @Override
    public void injectEnvFound(List<EnvFileEntry> envFileEntries, String envFileLocation) throws IOException {
        Map<Integer, EnvFileEntry> fileContents = new HashMap<>();

        envFileEntries.stream()
                .sorted(Comparator.comparingInt(EnvFileEntry::getLineNumber))
                .forEach(envFileEntry -> fileContents.put(envFileEntry.getLineNumber(), envFileEntry));

        List<String> fileLineEntries = fileContents.entrySet()
                .stream().sorted(Comparator.comparingInt(mapEntry -> mapEntry.getValue().getLineNumber()))
                .map(entry -> {
                    String fileEntry = entry.getValue().getName();
                    String envName = "";
                    String envValue = "";
                    if(entry.getValue().isValid() && !entry.getValue().isEnvValueSecret()) {
                        envName = entry.getValue().getName();
                        envValue = StringUtils.trimToEmpty(entry.getValue().getEnvValueToInject());
                        fileEntry = envName + "=" + envValue;
                    }
                    if(entry.getValue().isValid() && entry.getValue().isEnvValueSecret()) {
                        envName = entry.getValue().getName();
                        envValue = StringUtils.trimToEmpty(entry.getValue().getDefaultValue());
                        fileEntry = envName + "=" + envValue;
                    }
                    return fileEntry.trim();
                })
                .toList();

        Files.write(Path.of(envFileLocation + "-injected"), fileLineEntries, StandardCharsets.UTF_8);
    }

    @Override
    public void printReport(List<EnvFileEntry> envFileEntries) {
        logger.info("Invalid entries or empty lines");
        envFileEntries.stream()
                .filter(entry -> !entry.isValid())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries with no counterpart in .yml file");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(entry -> !entry.isFromYamlEnv())
                .filter(entry -> !entry.isPresentInYaml())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries that was injected with environment variables");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isInjected)
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Valid entries whose environment variable was a secret");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isEnvValueSecret)
                .filter(entry -> !entry.isFromYamlEnv())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Entries in .yaml file that was not present in .env file");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isFromYamlEnv)
                .filter(entry -> !entry.isEnvValueSecret())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("Secret entries in .yaml file that was not present in .env file");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isFromYamlEnv)
                .filter(EnvFileEntry::isEnvValueSecret)
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));
    }

    @Override
    public List<EnvFileEntry> addNewEnvFromYaml(List<EnvFileEntry> currentEnvFileEntries, List<YamlFileEnvEntry> yamlFileEnvEntries) {
        // add new EnvFileEntry for env in yaml that is not present in .env file
        List<String> envNames = currentEnvFileEntries.stream().map(EnvFileEntry::getName).toList();
        AtomicInteger lineNumbersForNewEnv = new AtomicInteger(currentEnvFileEntries.size() + 1);
        return yamlFileEnvEntries
                .stream()
                .filter(yamlFileEnvEntry -> !envNames.contains(yamlFileEnvEntry.envName()))
                .map(yamlFileEnvEntry -> new EnvFileEntry(yamlFileEnvEntry.envName(),
                        null,
                        yamlFileEnvEntry.envValue(),
                        true,
                        false,
                        lineNumbersForNewEnv.getAndIncrement(),
                        yamlFileEnvEntry.isSecret(),
                        true,
                        false
                ))
                .toList();
    }

    @Override
    public List<EnvFileEntry> populateEnvFileEntriesWithValuesFromYaml(List<EnvFileEntry> currentEnvFileEnties, List<YamlFileEnvEntry> yamlFileEnvEntries) {
        // populate the EnvFileEntry with the values present in yaml file
        for(EnvFileEntry entry : currentEnvFileEnties) {
            for(YamlFileEnvEntry yamlEntry : yamlFileEnvEntries) {
                if(Objects.nonNull(entry.getName()) && Objects.nonNull(yamlEntry.envName())) {
                    if(entry.getName().equals(yamlEntry.envName()) && !yamlEntry.isSecret()) {
                        entry.setEnvValueToInject(yamlEntry.envValue());
                        entry.setEnvValueSecret(false);
                        entry.setInjected(true);
                        entry.setPresentInYaml(true);
                    }
                    if(entry.getName().equals(yamlEntry.envName()) && yamlEntry.isSecret()) {
                        entry.setEnvValueSecret(true);
                        entry.setPresentInYaml(true);
                    }
                }
            }
        }
        return currentEnvFileEnties;
    }
}
