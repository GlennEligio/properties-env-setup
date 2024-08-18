package com.glenneligio.service;

import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.PropertiesFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Files;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class PropertiesServiceImplTest {

    private static final String VALID_ENV_FILE_NAME = "src/test/resources/application.properties";
    private static final String VALID_ENV_WITH_NO_CONTENT_FILE_NAME = "src/test/resources/application-no-content.properties";
    private static final String NON_EXISTENT_FILE = "src/test/resources/application-non-existent.properties";
    private static final String EXPECTED_ENV_FILE = "src/test/resources/application-expected.properties";
    private PropertiesFileEntry p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11;
    private List<PropertiesFileEntry> validPropertyFileEntries = new ArrayList<>();
    private YamlFileEnvEntry y0, y1, y2;
    private List<YamlFileEnvEntry> validYamlEnvFileEntries = new ArrayList<>();

    @BeforeEach
    void setupEach() {
        populatedValidEnvFileEntries();
        populateValidYamlEnvFileEntries();
    }

    private void populatedValidEnvFileEntries() {
        // valid env injected with default value
        p0 = new PropertiesFileEntry("db.host",
                "DB_HOST",
                "defaultLocalHost",
                true,
                true,
                1,
                false);
        // comment line
        p1 = new PropertiesFileEntry(null,
                null,
                null,
                false,
                false,
                2,
                false);
        // valid env injected with default value
        p2 = new PropertiesFileEntry("db.port",
                "DB_PORT",
                "defaultPort",
                true,
                true,
                3,
                false);
        // valid env injected with default value
        p3 = new PropertiesFileEntry("secret.api.key",
                "SECRET_API_KEY",
                "defaultSec=retApiKey",
                true,
                true,
                4,
                false);
        // valid entry with default value but not injected
        p4 = new PropertiesFileEntry("property.no.env.used",
                null,
                "STATIC_VALUE",
                true,
                false,
                5,
                false);
        // valid env injected with no default value
        p5 = new PropertiesFileEntry("another.env",
                "JUST_AN_ENV",
                null,
                true,
                true,
                6,
                false);
        // valid env injected with no default value
        p6 = new PropertiesFileEntry("env.notpresent.in.yaml",
                "ENV_NOT_IN_YAML",
                null,
                true,
                true,
                7,
                false);
        // valid env injected with no default value but with inline comment
        p7 = new PropertiesFileEntry("property.entry.with.comment",
                "COMMENT_VALUE",
                null,
                true,
                true,
                8,
                false);
        // property with invalid syntax for property value
        p8 = new PropertiesFileEntry("invalid.prop.entry.value.syntax=${qweqweqweqwe}}",
                null,
                null,
                false,
                false,
                9,
                false);
        // blank line
        p9 = new PropertiesFileEntry(null,
                null,
                null,
                false,
                false,
                10,
                false);
        // valid entry but with no default value and not injected
        p10 = new PropertiesFileEntry("property.with.no.value",
                null,
                "",
                true,
                false,
                11,
                false);
        // invalid entry
        p11 = new PropertiesFileEntry("invalid.property",
                null,
                null,
                false,
                false,
                12,
                false);
        validPropertyFileEntries.addAll(Lists.list(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11));
    }

    private void populateValidYamlEnvFileEntries() {
        // nonSecretYamlEnvEntry1
        y0 = new YamlFileEnvEntry("DB_HOST", "10.950.54.10", false);
        // nonSecretYamlEnvEntry2
        y1 = new YamlFileEnvEntry("DB_PORT", "1443", false);
        // secretYamlEnvEntry1
        y2 = new YamlFileEnvEntry("SECRET_API_KEY", null, true);
        validYamlEnvFileEntries = new ArrayList<>(Lists.list(y0, y1, y2));
    }

    // getPropertiesFileEntriesFromPropertiesFile
    // PropertyFileEntry list should match the output
    @Test
    void givenValidPropertiesFile_returnsListOfPropertiesCorrespondingToContent() throws IOException {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> result = propertiesService.getPropertiesFileEntriesFromPropertiesFile(VALID_ENV_FILE_NAME);

        Assertions.assertEquals(validPropertyFileEntries, result);
    }

    // populateEnvFileEntriesWithValuesFromYaml
    // the PropertyFileEntry list content should be updated properly based on YamlFileEnvEntry list
    @Test
    void givenValidListOfPropertyFileEntryAndYamlFileEnvEntry_withMatchingEnvFromYaml_returnsUpdatedPropertyFileEntries() {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> updatedPropertyFileEntries = new ArrayList<>(validPropertyFileEntries);
        PropertiesFileEntry updatedP0 = updatedPropertyFileEntries.get(0);
        updatedP0.setEnvValueToInject(y0.getEnvValue());
        updatedP0.setEnvValueSecret(false);
        updatedP0.setPresentInYaml(true);
        updatedPropertyFileEntries.set(0, updatedP0);
        PropertiesFileEntry updatedP2 = updatedPropertyFileEntries.get(2);
        updatedP2.setEnvValueToInject(y1.getEnvValue());
        updatedP2.setEnvValueSecret(false);
        updatedP2.setPresentInYaml(true);
        updatedPropertyFileEntries.set(2, updatedP2);
        PropertiesFileEntry updatedP3 = updatedPropertyFileEntries.get(3);
        updatedP3.setEnvValueToInject(y2.getEnvValue());
        updatedP3.setEnvValueSecret(true);
        updatedP3.setPresentInYaml(true);
        updatedPropertyFileEntries.set(3, updatedP3);

        List<PropertiesFileEntry> result = propertiesService.populateEnvFileEntriesWithValuesFromYaml(validPropertyFileEntries, validYamlEnvFileEntries);

        Assertions.assertEquals(updatedPropertyFileEntries, result);
    }


    // injectEnvFound
    // only inject entries that are valid, is injected, and not secret
    @Test
    void givenListOfPropertyFileEntryButNonExistingPropertyFile_throwRuntimeException() throws IOException {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> updatedPropertyFileEntries = new ArrayList<>(validPropertyFileEntries);
        PropertiesFileEntry updatedP0 = updatedPropertyFileEntries.get(0);
        updatedP0.setEnvValueToInject(y0.getEnvValue());
        updatedP0.setEnvValueSecret(false);
        updatedP0.setPresentInYaml(true);
        updatedPropertyFileEntries.set(0, updatedP0);
        PropertiesFileEntry updatedP2 = updatedPropertyFileEntries.get(2);
        updatedP2.setEnvValueToInject(y1.getEnvValue());
        updatedP2.setEnvValueSecret(false);
        updatedP2.setPresentInYaml(true);
        updatedPropertyFileEntries.set(2, updatedP2);

        Assertions.assertThrows(RuntimeException.class, () -> propertiesService.injectEnvFound(updatedPropertyFileEntries, NON_EXISTENT_FILE));
    }

    @Test
    void givenListOfPropertyFileEntryAndValidPropertyFile_createsNewPropertyFileEntryWithCorrectContent() throws IOException {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> updatedPropertyFileEntries = new ArrayList<>(validPropertyFileEntries);
        PropertiesFileEntry updatedP0 = updatedPropertyFileEntries.get(0);
        updatedP0.setEnvValueToInject(y0.getEnvValue());
        updatedP0.setEnvValueSecret(false);
        updatedP0.setPresentInYaml(true);
        updatedP0.setInjected(true);
        updatedPropertyFileEntries.set(0, updatedP0);
        PropertiesFileEntry updatedP2 = updatedPropertyFileEntries.get(2);
        updatedP2.setEnvValueToInject(y1.getEnvValue());
        updatedP2.setEnvValueSecret(false);
        updatedP2.setPresentInYaml(true);
        updatedP2.setInjected(true);
        updatedPropertyFileEntries.set(2, updatedP2);

        propertiesService.injectEnvFound(updatedPropertyFileEntries, VALID_ENV_FILE_NAME);

        File file = new File(VALID_ENV_FILE_NAME + "-injected");
        File expectedFile = new File(EXPECTED_ENV_FILE);
        String fileResultContent = Files.contentOf(file, StandardCharsets.UTF_8);
        String fileExpectedContent = Files.contentOf(expectedFile, StandardCharsets.UTF_8);
        Assertions.assertTrue(file.exists());
        Assertions.assertEquals(fileExpectedContent, fileResultContent);
    }

    // printReport
    // will not throw error
    @Test
    void givenListOfPropertiesFileEntry_printReportWillNotThrowError() {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> updatedPropertyFileEntries = new ArrayList<>(validPropertyFileEntries);
        PropertiesFileEntry updatedP0 = updatedPropertyFileEntries.get(0);
        updatedP0.setEnvValueToInject(y0.getEnvValue());
        updatedP0.setEnvValueSecret(false);
        updatedP0.setPresentInYaml(true);
        updatedP0.setInjected(true);
        updatedPropertyFileEntries.set(0, updatedP0);
        PropertiesFileEntry updatedP2 = updatedPropertyFileEntries.get(2);
        updatedP2.setEnvValueToInject(y1.getEnvValue());
        updatedP2.setEnvValueSecret(false);
        updatedP2.setPresentInYaml(true);
        updatedP2.setInjected(true);
        updatedPropertyFileEntries.set(2, updatedP2);
        PropertiesFileEntry updatedP3 = updatedPropertyFileEntries.get(3);
        updatedP3.setEnvValueToInject(y2.getEnvValue());
        updatedP3.setEnvValueSecret(true);
        updatedP3.setPresentInYaml(true);
        updatedPropertyFileEntries.set(3, updatedP3);

        Assertions.assertDoesNotThrow(() -> propertiesService.printReport(updatedPropertyFileEntries));
    }
}
