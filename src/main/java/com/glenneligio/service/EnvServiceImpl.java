package com.glenneligio.service;

import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

        List<String> envFileLines = Files.readAllLines(Paths.get(envFileLocation));

        // initialize line number, to be used when populating new .env file
        int lineNumber = 1;

        // iterate through the env files lines
        for(String envFileEntry : envFileLines) {
            String envFileEntryValue = envFileEntry;

            int commentCharIndex = envFileEntry.indexOf("#");

            // removed substring after # to clean up comments
            if(commentCharIndex != -1) {
                logger.info("Cleaning the line by removing comment in line {}", lineNumber);
                envFileEntryValue = envFileEntry.substring(0, commentCharIndex).trim();
            }

            // check if the remaining string is not empty
            // if it is, skip it by adding EnvFileEntry with null values, then continue
            if(envFileEntryValue.trim().length() == 0) {
                logger.debug("Empty line, skipped");
                EnvFileEntry entry = new EnvFileEntry(envFileEntry, null,null, false, lineNumber);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            int equalsIndex = envFileEntryValue.indexOf("=");

            // check if "=" is present
            // if it isn't (index = -1), add EnvFileEntry with null fields, and continue
            if(equalsIndex == -1) {
                logger.debug("Invalid entry, skipped: {}", envFileEntryValue);
                EnvFileEntry entry = new EnvFileEntry(envFileEntryValue, null, null, false, lineNumber);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            // get env name and value using equal index
            String envName = envFileEntryValue.substring(0, equalsIndex).trim();
            String envValue = envFileEntryValue.substring(equalsIndex + 1).trim();
            logger.debug("envName: {}", envName);
            logger.debug("envValue: {}", envValue);

            // check if env value is empty string
            // if it is, add EnvFileEntry but with no default value field, and continue
            if(envValue.trim().length() == 0) {
                logger.debug("Entry have no default value: {}", envName);
                EnvFileEntry entry = new EnvFileEntry(envName, "", null, true, lineNumber);
                envFileEntries.add(entry);
                lineNumber++;
                continue;
            }

            // case where entry have default value
            // add EnvFileEntry with default value
            logger.debug("Entry have default value: {}", envName);
            EnvFileEntry entry = new EnvFileEntry(envName, envValue, null, true, lineNumber);
            envFileEntries.add(entry);
            lineNumber++;
        }

        return envFileEntries;
    }

    @Override
    public void injectEnvFound(List<EnvFileEntry> envFileEntries, String envFileLocation) throws IOException {
        Map<Integer, EnvFileEntry> fileContents = new HashMap<>();

        // sort the envFileEntries by line number in ascending order
        // then add them to the fileContents, where line number is key and EnvFileEntry is value
        envFileEntries.stream()
                .sorted(Comparator.comparingInt(EnvFileEntry::getLineNumber))
                .forEach(envFileEntry -> fileContents.put(envFileEntry.getLineNumber(), envFileEntry));

        // Iterate through the fileContents map (sorted in key - line number - in ascending order)
        List<String> fileLineEntries = fileContents.entrySet()
                .stream()
                // sort by line number again in descending order
                .sorted(Comparator.comparingInt(mapEntry -> mapEntry.getValue().getLineNumber()))
                .map(entry -> {
                    logger.info("Entry value: {}", entry.getValue());
                    EnvFileEntry envFileEntry = entry.getValue();
                    String fileEntry = entry.getValue().getName();

                    // initialize the envName and envValue to use when populating the
                    String envName = "";
                    String envValue = "";

                    // if valid, injected with value from yaml, and env from yaml is not a secret
                    // replace envName and envValue variable with the name and value from yaml
                    if(envFileEntry.isValid() && envFileEntry.isInjected() && !envFileEntry.isEnvValueSecret()) {
                        envName = envFileEntry.getName();
                        envValue = StringUtils.trimToEmpty(envFileEntry.getEnvValueToInject());
                        fileEntry = envName + "=" + envValue;
                    }
                    // else if valid, but was not injected (due to value not present in yaml file)
                    // use the default value
                    else if(envFileEntry.isValid() && !envFileEntry.isInjected()) {
                        envName = envFileEntry.getName();
                        envValue = envFileEntry.getDefaultValue();
                        fileEntry = envName + "=" + envValue;
                    }
                    return fileEntry.trim();
                })
                .collect(Collectors.toList());

        File injectedFile = new File(envFileLocation + "-injected");

        // remove the existing injectedFile, and create new file
        if(injectedFile.exists()) {
            injectedFile.delete();
            injectedFile.createNewFile();
        }

        Files.write(Paths.get(envFileLocation + "-injected"), fileLineEntries, StandardCharsets.UTF_8);
    }

    @Override
    public void printReport(List<EnvFileEntry> envFileEntries) {
        if(envFileEntries.isEmpty()) {
            logger.info("Empty list of env file entries. Will not be printing report");
            return;
        }

        logger.info("***************** Invalid entries or empty lines ************************************");
        envFileEntries.stream()
                .filter(entry -> !entry.isValid())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("***************** Valid entries with no counterpart in .yml file ********************");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(entry -> !entry.isFromYamlEnv())
                .filter(entry -> !entry.isPresentInYaml())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("***************** Valid entries that was injected with environment variables ********");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isInjected)
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("***************** Valid entries whose environment variable was a secret *************");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isEnvValueSecret)
                .filter(entry -> !entry.isFromYamlEnv())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("***************** Entries in .yaml file that was not present in .env file ************");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isFromYamlEnv)
                .filter(entry -> !entry.isEnvValueSecret())
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));

        logger.info("***************** Secret entries in .yaml file that was not present in .env file *****");
        envFileEntries.stream()
                .filter(EnvFileEntry::isValid)
                .filter(EnvFileEntry::isFromYamlEnv)
                .filter(EnvFileEntry::isEnvValueSecret)
                .forEach(entry -> logger.info("{}. {}", entry.getLineNumber(), entry.getName()));
    }

    @Override
    public List<EnvFileEntry> addNewEnvFromYaml(List<EnvFileEntry> currentEnvFileEntries, List<YamlFileEnvEntry> yamlFileEnvEntries) {
        // add new EnvFileEntry for env in yaml that is not present in .env file
        // get all ENV names on current EnvFileEntries list (populated using yaml file)
        List<String> envNames = currentEnvFileEntries.stream().map(EnvFileEntry::getName).collect(Collectors.toList());

        // initialize the line number so that they are after the last line in the env file
        AtomicInteger lineNumbersForNewEnv = new AtomicInteger(currentEnvFileEntries.size() + 1);

        // iterate through all yaml env file entries
        return yamlFileEnvEntries
                .stream()
                // filter out env entry in yaml file that are NOT present in current EnvFileEntry list
                .filter(yamlFileEnvEntry -> !envNames.contains(yamlFileEnvEntry.getEnvName()))
                // for each env entry from yaml fie, create new EnvFileEntry object
                .map(yamlFileEnvEntry -> new EnvFileEntry(yamlFileEnvEntry.getEnvName(),
                        null,
                        yamlFileEnvEntry.getEnvValue(),
                        true,
                        false,
                        lineNumbersForNewEnv.getAndIncrement(),
                        yamlFileEnvEntry.isSecret(),
                        true,
                        false
                ))
                // return it
                .collect(Collectors.toList());

    }

    @Override
    public List<EnvFileEntry> populateEnvFileEntriesWithValuesFromYaml(List<EnvFileEntry> currentEnvFileEnties, List<YamlFileEnvEntry> yamlFileEnvEntries) {
        // populate the EnvFileEntry with the values present in yaml file
        // iterate through all EnvFileEntry read from the env file
        for(EnvFileEntry entry : currentEnvFileEnties) {
            // iterate through all ENV in yaml file
            for(YamlFileEnvEntry yamlEntry : yamlFileEnvEntries) {
                // check if any ENV from yaml file matches on the current EnvFileEntry
                if(Objects.nonNull(entry.getName()) && Objects.nonNull(yamlEntry.getEnvName())) {
                    // if names matches and ENV from yaml file is not a secret
                    // add the ENV yaml values on EnvFileEntry
                    if(entry.getName().equals(yamlEntry.getEnvName()) && !yamlEntry.isSecret()) {
                        entry.setEnvValueToInject(yamlEntry.getEnvValue());
                        entry.setEnvValueSecret(false);
                        entry.setInjected(true);
                        entry.setPresentInYaml(true);
                    }
                    // else if name matches and ENV from yaml file is a secret
                    // set the EnvFileEntry fields to true
                    else if(entry.getName().equals(yamlEntry.getEnvName()) && yamlEntry.isSecret()) {
                        entry.setEnvValueSecret(true);
                        entry.setPresentInYaml(true);
                    }
                }
            }
        }
        return currentEnvFileEnties;
    }
}
