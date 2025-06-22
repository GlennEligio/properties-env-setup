package com.glenneligio.commands;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Group;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import com.glenneligio.service.PropertiesService;
import com.glenneligio.service.PropertiesServiceImpl;
import com.glenneligio.service.YamlService;
import com.glenneligio.service.YamlServiceImpl;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Command(name = "properties", description = "Setup the application.properties file using k8s yaml")
@Group(name = "setup")
public class PropertiesSetup implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesSetup.class);

    @Inject
    private HelpOption<PropertiesSetup> help;

    @Option(name = {"-p" , "--properties"},
            description = "application.properties to populate")
    @Required
    protected String propertiesFile;

    @Option(name = {"-y" , "--yaml"},
            description = "k8s yaml file where environments are declared")
    @Required
    protected String yamlFile;

    @Option(name = {"-c" , "--container"},
            description = "Container name where env files are fetched")
    @Required
    protected String containerName;

    public PropertiesSetup(String propertiesFile, String yamlFile, String containerImageName) {
        this.propertiesFile = propertiesFile;
        this.yamlFile = yamlFile;
        this.containerName = containerImageName;
    }

    public PropertiesSetup() {
        // Default constructor for dependency injection
    }

    @SneakyThrows
    @Override
    public void run() {
        System.out.println(String.format("Properties file to be populated: %s", propertiesFile));
        System.out.println(String.format("K8s yaml file to be used: %s", yamlFile));
        System.out.println(String.format("Container name where env file is fetched: %s", containerName));

        // Reading the properties file
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> propertiesEntries = propertiesService.getPropertiesFileEntriesFromPropertiesFile(propertiesFile);
        logger.debug("Properties file entries");
        for(PropertiesFileEntry entry : propertiesEntries) {
            logger.debug("Entry - name: {}, defaultValue: {}, envInjected: {}, isValid: {}, isInjected: {}",
                    StringUtils.trimToEmpty(entry.getName()),
                    StringUtils.trimToEmpty(entry.getDefaultValue()),
                    StringUtils.trimToEmpty(entry.getEnvUsed()),
                    entry.isValid(),
                    entry.isValueInjected());
        }

        YamlService yamlService = new YamlServiceImpl();
        List<YamlFileEnvEntry> yamlEnvEntries = yamlService.getYamlFileEnvEntries(yamlFile, containerName);
        logger.debug("YAML env entries");
        for(YamlFileEnvEntry entry : yamlEnvEntries) {
            logger.debug("Entry - name: {}, value: {}, isSecret: {}", StringUtils.trimToEmpty(entry.getEnvName()), StringUtils.trimToEmpty(entry.getEnvValue()), entry.isSecret());
        }

        List<PropertiesFileEntry> populatedPropEntries = propertiesService.populateEnvFileEntriesWithValuesFromYaml(propertiesEntries, yamlEnvEntries);

        propertiesService.injectEnvFound(populatedPropEntries, propertiesFile);
        propertiesService.printReport(populatedPropEntries);
    }
}
