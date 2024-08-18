package com.glenneligio.commands;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Group;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import com.glenneligio.service.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Command(name = "env", description = "Setup the .env file using k8s yaml")
@Group(name = "setup")
public class EnvSetup implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(EnvSetup.class);

    @Inject
    private HelpOption<EnvSetup> help;

    @Option(name = {"-e" , "--env"}, description = ".env to populate")
    @Required
    protected String envFile;

    @Option(name = {"-y" , "--yaml"}, description = "k8s yaml file where environments are declared")
    @Required
    protected String yamlFile;

    @Option(name = {"-i" , "--image"},
            description = "Image name of the container used")
    @Required
    protected String containerImageName;

    @SneakyThrows
    @Override
    public void run() {
        logger.info("Env file to be populated: {}", envFile);
        logger.info("K8s yaml file to be used: {}", yamlFile);

        // Reading the properties file
        EnvService envService = new EnvServiceImpl();
        List<EnvFileEntry> envFileEntries = envService.readOrCreateEnvFile(envFile);
        logger.info("Env file entries");
        for(EnvFileEntry entry : envFileEntries) {
            logger.info("Entry - name: {}, defaultValue: {}, isValid: {}",
                    StringUtils.trimToEmpty(entry.getName()),
                    StringUtils.trimToEmpty(entry.getDefaultValue()),
                    entry.isValid());
        }

        YamlService yamlService = new YamlServiceImpl();
        List<YamlFileEnvEntry> yamlEnvEntries = yamlService.getYamlFileEnvEntries(yamlFile, containerImageName);
        logger.info("YAML env entries");
        for(YamlFileEnvEntry entry : yamlEnvEntries) {
            logger.info("Entry - name: {}, value: {}, isSecret: {}", StringUtils.trimToEmpty(entry.getEnvName()), StringUtils.trimToEmpty(entry.getEnvValue()), entry.isSecret());
        }

        envService.populateEnvFileEntriesWithValuesFromYaml(envFileEntries, yamlEnvEntries);

        // add new EnvFileEntry for env in yaml that is not present in .env file
        List<EnvFileEntry> missingEnvFromYaml = envService.addNewEnvFromYaml(envFileEntries, yamlEnvEntries);

        // add new env from .yml file that was not present in .env file
        envFileEntries.addAll(missingEnvFromYaml);
        envService.injectEnvFound(envFileEntries, envFile);
        envService.printReport(envFileEntries);
    }
}
