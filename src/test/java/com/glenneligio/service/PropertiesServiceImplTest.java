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
    private PropertiesFileEntry p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13;
    private List<PropertiesFileEntry> validPropertyFileEntries = new ArrayList<>();
    private YamlFileEnvEntry y0, y1, y2, y3, y4, y5;
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
        // valid env injected with no default value
        p7 = new PropertiesFileEntry("prop.with.injected.env.but.no.default.value",
                "ENV_NO_DEFAULT_VALUE",
                "",
                true,
                true,
                8,
                false);
        // valid env injected with no default value
        p8 = new PropertiesFileEntry("prop.with.injected.env.as.lowercases",
                "lower_case_env",
                null,
                true,
                true,
                9,
                false);
        // valid env injected with no default value but with inline comment
        p9 = new PropertiesFileEntry("property.entry.with.comment",
                "COMMENT_VALUE",
                null,
                true,
                true,
                10,
                false);
        // property with invalid syntax for property value
        p10 = new PropertiesFileEntry("invalid.prop.entry.value.syntax=${qweqweqweqwe}}",
                null,
                null,
                false,
                false,
                11,
                false);
        // blank line
        p11 = new PropertiesFileEntry(null,
                null,
                null,
                false,
                false,
                12,
                false);
        // valid entry but with no default value and not injected
        p12 = new PropertiesFileEntry("property.with.no.value",
                null,
                "",
                true,
                false,
                13,
                false);
        // invalid entry
        p13 = new PropertiesFileEntry("invalid.property",
                null,
                null,
                false,
                false,
                14,
                false);
        validPropertyFileEntries.addAll(Lists.list(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13));
    }

    private void populateValidYamlEnvFileEntries() {
        // nonSecretYamlEnvEntry1
        y0 = new YamlFileEnvEntry("DB_HOST", "10.950.54.10", false);
        // nonSecretYamlEnvEntry2
        y1 = new YamlFileEnvEntry("DB_PORT", "1443", false);
        // nonSecretYamlEnvEntry3
        y2 = new YamlFileEnvEntry("JUST_AN_ENV", "justAnEnvValue", false);
        // nonSecretYamlEnvEntry4
        y3 = new YamlFileEnvEntry("ENV_NO_DEFAULT_VALUE", "envNoDefaultValue", false);
        // nonSecretYamlEnvEntry4
        y4 = new YamlFileEnvEntry("lower_case_env", "lowerCaseEnv", false);
        // secretYamlEnvEntry1
        y5 = new YamlFileEnvEntry("SECRET_API_KEY", null, true);
        validYamlEnvFileEntries = new ArrayList<>(Lists.list(y0, y1, y2, y3, y4, y5));
    }

    // getPropertiesFileEntriesFromPropertiesFile
    // PropertyFileEntry list should match the output
    @Test
    void givenValidPropertiesFile_returnsListOfPropertiesCorrespondingToContent() throws IOException {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> result = propertiesService.getPropertiesFileEntriesFromPropertiesFile(VALID_ENV_FILE_NAME);

        Assertions.assertEquals(validPropertyFileEntries, result);
    }

    // populateEnvFileEntriesWithValuesFromYaml()
    // the PropertyFileEntry list content should be updated properly based on YamlFileEnvEntry list
    @Test
    void givenValidListOfPropertyFileEntryAndYamlFileEnvEntry_withMatchingEnvFromYaml_returnsUpdatedPropertyFileEntries() {
        PropertiesService propertiesService = new PropertiesServiceImpl();
        List<PropertiesFileEntry> updatedPropertyFileEntries = new ArrayList<>(validPropertyFileEntries);

        // edit PropertiesFileEntry to have values from YamlEntryFile
        // with value from yaml that is not secret
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

        PropertiesFileEntry updatedP5 = updatedPropertyFileEntries.get(5);
        updatedP5.setEnvValueToInject(y2.getEnvValue());
        updatedP5.setEnvValueSecret(false);
        updatedP5.setPresentInYaml(true);
        updatedPropertyFileEntries.set(5, updatedP5);

        PropertiesFileEntry updatedP7 = updatedPropertyFileEntries.get(7);
        updatedP7.setEnvValueToInject(y3.getEnvValue());
        updatedP7.setEnvValueSecret(false);
        updatedP7.setPresentInYaml(true);
        updatedPropertyFileEntries.set(7, updatedP7);

        PropertiesFileEntry updatedP8 = updatedPropertyFileEntries.get(8);
        updatedP8.setEnvValueToInject(y4.getEnvValue());
        updatedP8.setEnvValueSecret(false);
        updatedP8.setPresentInYaml(true);
        updatedPropertyFileEntries.set(8, updatedP8);

        // env was a secret in yaml
        PropertiesFileEntry updatedP3 = updatedPropertyFileEntries.get(3);
        updatedP3.setEnvValueSecret(true);
        updatedP3.setPresentInYaml(true);
        updatedPropertyFileEntries.set(3, updatedP3);

        List<PropertiesFileEntry> result = propertiesService.populateEnvFileEntriesWithValuesFromYaml(validPropertyFileEntries, validYamlEnvFileEntries);

        Assertions.assertEquals(updatedPropertyFileEntries, result);
    }


    // injectEnvFound()
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

        // edit PropertiesFileEntry to have values from YamlEntryFile
        // with value from yaml that is not secret
        PropertiesFileEntry updatedP0 = updatedPropertyFileEntries.get(0);
        updatedP0.setValid(true);
        updatedP0.setValueInjected(true);
        updatedP0.setInjected(true);
        updatedP0.setEnvValueToInject(y0.getEnvValue());
        updatedP0.setEnvValueSecret(false);
        updatedP0.setPresentInYaml(true);
        updatedPropertyFileEntries.set(0, updatedP0);

        PropertiesFileEntry updatedP2 = updatedPropertyFileEntries.get(2);
        updatedP2.setValueInjected(true);
        updatedP2.setValid(true);
        updatedP2.setInjected(true);
        updatedP2.setEnvValueToInject(y1.getEnvValue());
        updatedP2.setEnvValueSecret(false);
        updatedP2.setPresentInYaml(true);
        updatedPropertyFileEntries.set(2, updatedP2);

        PropertiesFileEntry updatedP5 = updatedPropertyFileEntries.get(5);
        updatedP5.setValueInjected(true);
        updatedP5.setValid(true);
        updatedP5.setInjected(true);
        updatedP5.setEnvValueToInject(y2.getEnvValue());
        updatedP5.setEnvValueSecret(false);
        updatedP5.setPresentInYaml(true);
        updatedPropertyFileEntries.set(5, updatedP5);

        PropertiesFileEntry updatedP7 = updatedPropertyFileEntries.get(7);
        updatedP7.setValueInjected(true);
        updatedP7.setValid(true);
        updatedP7.setInjected(true);
        updatedP7.setEnvValueToInject(y3.getEnvValue());
        updatedP7.setEnvValueSecret(false);
        updatedP7.setPresentInYaml(true);
        updatedPropertyFileEntries.set(7, updatedP7);

        PropertiesFileEntry updatedP8 = updatedPropertyFileEntries.get(8);
        updatedP8.setValueInjected(true);
        updatedP8.setValid(true);
        updatedP8.setInjected(true);
        updatedP8.setEnvValueToInject(y4.getEnvValue());
        updatedP8.setEnvValueSecret(false);
        updatedP8.setPresentInYaml(true);
        updatedPropertyFileEntries.set(8, updatedP8);

        // env was a secret in yaml
        PropertiesFileEntry updatedP3 = updatedPropertyFileEntries.get(3);
        updatedP3.setEnvValueSecret(true);
        updatedP3.setPresentInYaml(true);
        updatedPropertyFileEntries.set(3, updatedP3);

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
