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

    @Option(name = {"-i" , "--image"},
            description = "Image name of the container used")
    @Required
    protected String containerImageName;

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Properties file to be populated: {}", propertiesFile);
        logger.info("K8s yaml file to be used: {}", yamlFile);
        logger.info("Image name of the container where env file is fetched: {}", containerImageName);

        // Reading the properties file
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> propertiesEntries = propertiesService.getPropertiesFileEntriesFromPropertiesFile(propertiesFile);
        logger.info("Properties file entries");
        for(PropertiesFileEntry entry : propertiesEntries) {
            logger.info("Entry - name: {}, defaultValue: {}, envInjected: {}, isValid: {}, isInjected: {}",
                    StringUtils.trimToEmpty(entry.getName()),
                    StringUtils.trimToEmpty(entry.getDefaultValue()),
                    StringUtils.trimToEmpty(entry.getEnvUsed()),
                    entry.isValid(),
                    entry.isValueInjected());
        }

        YamlService yamlService = new YamlServiceImpl();
        List<YamlFileEnvEntry> yamlEnvEntries = yamlService.getYamlFileEnvEntries(yamlFile, containerImageName);
        logger.info("YAML env entries");
        for(YamlFileEnvEntry entry : yamlEnvEntries) {
            logger.info("Entry - name: {}, value: {}, isSecret: {}", StringUtils.trimToEmpty(entry.envName()), StringUtils.trimToEmpty(entry.envValue()), entry.isSecret());
        }

        List<PropertiesFileEntry> populatedPropEntries = propertiesService.populateEnvFileEntriesWithValuesFromYaml(propertiesEntries, yamlEnvEntries);

        propertiesService.injectEnvFound(populatedPropEntries, propertiesFile);
        propertiesService.printReport(populatedPropEntries);
    }
}
