package com.glenneligio.service;

import com.glenneligio.model.EnvFileEntry;
import com.glenneligio.model.YamlFileEnvEntry;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class EnvServiceImplTest {

    private static final String VALID_ENV_FILE_NAME = "src/test/resources/withValidEntries.env";
    private static final String VALID_ENV_WITH_NO_CONTENT_FILE_NAME = "src/test/resources/withNoEntries.env";
    private static final String NON_EXISTENT_FILE = "src/test/resources/non-existent.env";
    private static final String EXPECTED_ENV_FILE = "src/test/resources/expectedEnvFile.env";
    private EnvFileEntry e0, e1, e2, e3, e4, e5, e6;
    private List<EnvFileEntry> validEnvFileEntries = new ArrayList<>();
    private YamlFileEnvEntry y0, y1, y2;
    private List<YamlFileEnvEntry> validYamlEnvFileEntries = new ArrayList<>();

    @BeforeEach
    void setupEach() {
        populatedValidEnvFileEntries();
        populateValidYamlEnvFileEntries();
    }

    private void populatedValidEnvFileEntries() {
        // entryWithDefaultValue1
        e0 = new EnvFileEntry("DB_HOST", "defaultLocalHost", null, true, false, 1, false, false, false);
        // entryWithDefaultValue2
        e1 = new EnvFileEntry("DB_PORT", "defaultPort", null, true, false, 2, false, false, false);
        // emptyLine
        e2 = new EnvFileEntry("", null, null, false, false, 3, false, false, false);
        // invalidEntry
        e3 = new EnvFileEntry("INVALID_ENTRY", null, null, false, false, 4, false, false, false);
        // entryWithNoDefaultValue
        e4 = new EnvFileEntry("ENV_WITH_NO_DEFAULT_VALUE", "", null, true, false, 5, false, false, false);
        // entryWithDefaultValue3
        e5 = new EnvFileEntry("SECRET_API_KEY", "defaultSecretApiKey", null, true, false, 6, false, false, false);
        // entryWithNoDefaultValue2
        e6 = new EnvFileEntry("ENV_NOT_IN_YAML", "", null, true, false, 7, false, false, false);
        validEnvFileEntries = new ArrayList<>(List.of(e0,
                e1,
                e2,
                e3,
                e4,
                e5,
                e6));
    }

    private void populateValidYamlEnvFileEntries() {
        // nonSecretYamlEnvEntry1
        y0 = new YamlFileEnvEntry("DB_HOST", "10.950.54.10", false);
        // nonSecretYamlEnvEntry2
        y1 = new YamlFileEnvEntry("DB_PORT", "1443", false);
        // secretYamlEnvEntry1
        y2 = new YamlFileEnvEntry("SECRET_API_KEY", null, true);
        validYamlEnvFileEntries = new ArrayList<>(List.of(y0, y1, y2));
    }

    @Test
    void givenValidEnvFileWithEntries_returnListOfEnvFileEntriesCorrespondingToContent() throws IOException, URISyntaxException {
        EnvService envService = new EnvServiceImpl();
        List<EnvFileEntry> result = envService.readOrCreateEnvFile(VALID_ENV_FILE_NAME);

        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(validEnvFileEntries, result);
    }

    @Test
    void givenValidEnvFileWithNoEntry_returnEmptyListOfEnvFileEntries() throws IOException, URISyntaxException {
        EnvService envService = new EnvServiceImpl();
        List<EnvFileEntry> result = envService.readOrCreateEnvFile(VALID_ENV_WITH_NO_CONTENT_FILE_NAME);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void givenValidEnvFileWithNoEntry_returnEmptyListOfEnvFileEntriesAndCreateNewFile() throws IOException, URISyntaxException {
        EnvService envService = new EnvServiceImpl();
        List<EnvFileEntry> result = envService.readOrCreateEnvFile(NON_EXISTENT_FILE);

        Assertions.assertTrue(result.isEmpty());
        Assertions.assertTrue(new File(NON_EXISTENT_FILE).exists());
    }

    // populateEnvFileEntriesWithValuesFromYaml
    // when there's a non-secret and secret env found in yaml that matches the .env file
    @Test
    void givenListOfEnvFileEntriesAndYamlEnvEntries_whenThereIsMatchingNonSecretAndSecretEnv_returnsCorrectlyPopulatedEnvFileEntriesList() {
        EnvService envService = new EnvServiceImpl();
        EnvFileEntry updatedNonSecretEnvFile1 = new EnvFileEntry("DB_HOST", "defaultLocalHost", y0.envValue(), true, true, 1, false, false, true);
        EnvFileEntry updatedNonSecretEnvFile2 = new EnvFileEntry("DB_PORT", "defaultPort", y1.envValue(), true, true, 2, false, false, true);
        EnvFileEntry updatedSecretEnvFile1 = new EnvFileEntry("SECRET_API_KEY", "defaultSecretApiKey", null, true, false, 6, true, false, true);
        List<EnvFileEntry> expected = new ArrayList<>(List.copyOf(validEnvFileEntries));
        expected.set(0, updatedNonSecretEnvFile1);
        expected.set(1, updatedNonSecretEnvFile2);
        expected.set(5, updatedSecretEnvFile1);

        List<EnvFileEntry> result = envService.populateEnvFileEntriesWithValuesFromYaml(validEnvFileEntries, validYamlEnvFileEntries);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expected, result);
    }

    // addNewEnvFromYaml
    // when there's an env entry in yaml file that is not present in env file
    @Test
    void givenListOfEnvFileEntriesAndYamlEnvEntries_whenThereIsEnvInYamlNotPresentInEnvFile_returnsUpdatedListOfEnvFileEntries() {
        EnvService envService = new EnvServiceImpl();
        EnvFileEntry newNonSecretEnvFromYaml = new EnvFileEntry("NEW_NON_SECRET_ENV_FROM_YAML",
                null,
                "NON_SECRET_VALUE",
                true,
                false,
                8,
                false,
                true,
                false);
        EnvFileEntry newSecretEnvFromYaml = new EnvFileEntry("NEW_SECRET_ENV_FROM_YAML",
                null,
                null,
                true,
                false,
                9,
                true,
                true,
                false);
        YamlFileEnvEntry newEnvFromYamlFile = new YamlFileEnvEntry("NEW_NON_SECRET_ENV_FROM_YAML", "NON_SECRET_VALUE", false);
        YamlFileEnvEntry newSecretEnvFromYamlFile = new YamlFileEnvEntry("NEW_SECRET_ENV_FROM_YAML", null, true);
        List<EnvFileEntry> expected = new ArrayList<>(List.of(newNonSecretEnvFromYaml, newSecretEnvFromYaml));
        validYamlEnvFileEntries.add(newEnvFromYamlFile);
        validYamlEnvFileEntries.add(newSecretEnvFromYamlFile);

        List<EnvFileEntry> result = envService.addNewEnvFromYaml(validEnvFileEntries, validYamlEnvFileEntries);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(expected, result);
    }

    // injectEnvFound
    // inject values from list of env from yaml file to list of env file entries, and populate new .env file
    @Test
    void givenListOfEnvFileEntries_createsNewEnvFileWithCorrectEntries() throws IOException {
        EnvService envService = new EnvServiceImpl();
        EnvFileEntry updatedNonSecretEnvFile1 = new EnvFileEntry("DB_HOST", "defaultLocalHost", y0.envValue(), true, true, 1, false, false, true);
        EnvFileEntry updatedNonSecretEnvFile2 = new EnvFileEntry("DB_PORT", "defaultPort", y1.envValue(), true, true, 2, false, false, true);
        List<EnvFileEntry> modifiedEnvFileEntries = new ArrayList<>(List.copyOf(validEnvFileEntries));
        modifiedEnvFileEntries.set(0, updatedNonSecretEnvFile1);
        modifiedEnvFileEntries.set(1, updatedNonSecretEnvFile2);

        envService.injectEnvFound(modifiedEnvFileEntries, VALID_ENV_FILE_NAME);

        String expectedContent = Files.contentOf(new File(EXPECTED_ENV_FILE), StandardCharsets.UTF_8);
        String resultContent = Files.contentOf(new File(VALID_ENV_FILE_NAME + "-injected"), StandardCharsets.UTF_8);
        Assertions.assertEquals(expectedContent, resultContent);
    }

    // printReport
    // will not throw error
    @Test
    void givenListOfEnvFileEntries_printsReportAndDoesNotThrowError() {
        EnvService envService = new EnvServiceImpl();
        Assertions.assertDoesNotThrow(() -> envService.printReport(validEnvFileEntries));
    }

    @Test
    void givenEmptyListOfEnvFileEntries_printsReportAndDoesNotThrowError() {
        EnvService envService = new EnvServiceImpl();
        Assertions.assertDoesNotThrow(() -> envService.printReport(new ArrayList<>()));
    }
}
